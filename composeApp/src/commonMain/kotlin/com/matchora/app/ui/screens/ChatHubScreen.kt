package com.matchora.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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

private enum class ChatHubSection(val label: String) {
    MESSAGES("Mesajlar"),
    SHOWCASE("Anı Vitrini")
}

private enum class ShowcaseFeedMode(val label: String) {
    RANDOM_EXPLORE("Keşfet · Karışık Akış"),
    NEARBY_LOCATION("Konuma Göre Vitrin"),
    TOP_LIKED("En Çok Beğenilenler")
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
    onToggleLikeMemory: (String) -> Unit = {}
) {
    var activeId by remember(initialActiveThreadId) { mutableStateOf(initialActiveThreadId) }
    var activeSection by remember { mutableStateOf(ChatHubSection.MESSAGES) }
    var input by remember(activeId) { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableIntStateOf(0) }
    var showInfo by remember { mutableStateOf(false) }

    // Anı Vitrini & Fotoğraf Paylaşım State
    var showcaseMode by remember { mutableStateOf(ShowcaseFeedMode.RANDOM_EXPLORE) }
    var shuffleSeed by remember { mutableIntStateOf(1) }
    var selectedMemoryForDetail by remember { mutableStateOf<MatchMemoryPost?>(null) }
    var showCreateMemorySheet by remember { mutableStateOf(false) }
    var preselectedChatIdForMemory by remember { mutableStateOf<String?>(null) }

    val active = threads.firstOrNull { it.id == activeId }
    fun open(id: String?) {
        activeId = id
        onActiveThreadChange(id)
    }

    val displayedMemories = remember(memories, showcaseMode, shuffleSeed, currentCity, currentDistrict) {
        when (showcaseMode) {
            ShowcaseFeedMode.RANDOM_EXPLORE -> {
                if (shuffleSeed <= 1) memories
                else memories.sortedBy { (it.id.hashCode() * 31 + shuffleSeed * 97) and 0x7fffffff }
            }
            ShowcaseFeedMode.NEARBY_LOCATION -> {
                memories.sortedWith(
                    compareBy<MatchMemoryPost> {
                        when {
                            it.area.equals(currentDistrict, ignoreCase = true) -> 0
                            it.city.equals(currentCity, ignoreCase = true) -> 1
                            else -> 2
                        }
                    }.thenBy { it.distanceKm }
                )
            }
            ShowcaseFeedMode.TOP_LIKED -> {
                memories.sortedByDescending { it.likes }
            }
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
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    shadowElevation = 5.dp,
                    border = BorderStroke(1.dp, Color(0xFFE4E9F2)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(7.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x260B1220), spotColor = Color(0x360B1220))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(5.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ChatHubSection.entries.forEach { section ->
                            val selected = activeSection == section
                            val bg by animateColorAsState(
                                if (selected) MColors.Ink900 else Color.Transparent,
                                tween(220),
                                label = "chat_hub_section"
                            )
                            Surface(
                                onClick = {
                                    if (activeSection == section) localScrollToTop++ else activeSection = section
                                },
                                shape = RoundedCornerShape(13.dp),
                                color = bg,
                                shadowElevation = if (selected) 4.dp else 0.dp,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp, horizontal = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (section == ChatHubSection.MESSAGES) Icons.Rounded.Forum else Icons.Rounded.PhotoLibrary,
                                        contentDescription = null,
                                        tint = if (selected) Color(0xFFFFB37B) else MColors.Ink500,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = section.label,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) Color.White else MColors.Ink600
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

                            OutlinedTextField(
                                colors = premiumTextFieldColors(),
                                textStyle = MaterialTheme.typography.bodyMedium,
                                value = query,
                                onValueChange = { query = it },
                                placeholder = { Text("Sohbet veya oyuncu ara…") },
                                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = MColors.Ink500) },
                                trailingIcon = if (query.isNotEmpty()) {
                                    {
                                        IconButton(onClick = { query = "" }) {
                                            Icon(Icons.Rounded.Close, null, tint = MColors.Ink500, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                } else null,
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // 3C. SOHBET LİSTESİ
                items(filteredThreads, key = { it.id }) { thread ->
                    val hasPhotoInLast = thread.lastMessage?.attachedMemory != null
                    MCard(
                        modifier = Modifier.fillMaxWidth().animateItem(),
                        onClick = { open(thread.id) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ChatAvatar(thread)
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

                if (filteredThreads.isEmpty()) {
                    item {
                        MCard(Modifier.fillMaxWidth()) {
                            Text("Sohbet bulunamadı.", Modifier.padding(20.dp), color = MColors.Ink500)
                        }
                    }
                }
            } else {
                // ═══════════════════════════════════════════════════════════════
                // 4. INSTAGRAM KEŞFET TARZI SAHA ANILARI VİTRİNİ (KONUM & RANDOM AKIŞ)
                // ═══════════════════════════════════════════════════════════════
                // 4. INSTAGRAM KEŞFET TARZI SAHA ANILARI VİTRİNİ
                item {
                    var expandedFilter by remember { mutableStateOf(false) }
                    val filterOptions = listOf(
                        ShowcaseFeedMode.NEARBY_LOCATION to "Konuma Göre",
                        ShowcaseFeedMode.RANDOM_EXPLORE to "Karışık",
                        ShowcaseFeedMode.TOP_LIKED to "En Çok Beğenilen"
                    )
                    val currentLabel = filterOptions.firstOrNull { it.first == showcaseMode }?.second ?: "Konuma Göre"

                    MCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Tek Açılır Menü (Konuma Göre / Karışık / En Çok Beğenilen)
                            Box(modifier = Modifier.weight(1f)) {
                                Surface(
                                    onClick = { expandedFilter = true },
                                    shape = RoundedCornerShape(13.dp),
                                    color = Color(0xFFF8FAFC),
                                    shadowElevation = 2.dp,
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (expandedFilter) MColors.Brand.copy(alpha = 0.6f) else Color(0xFFDCE4F0)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp)
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
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MColors.Ink900,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Icon(
                                            imageVector = if (expandedFilter) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = MColors.Ink700,
                                            modifier = Modifier.size(19.dp)
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
                                                showcaseMode = mode
                                                expandedFilter = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Fotoğraf Ekle Butonu
                            Surface(
                                onClick = {
                                    preselectedChatIdForMemory = null
                                    showCreateMemorySheet = true
                                },
                                shape = RoundedCornerShape(13.dp),
                                color = MColors.Ink900,
                                shadowElevation = 4.dp,
                                border = BorderStroke(1.dp, Color(0xFFFFB37B).copy(alpha = 0.45f)),
                                modifier = Modifier.height(46.dp)
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
                    }
                }

                if (displayedMemories.isEmpty()) {
                    item {
                        MCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Henüz saha anısı yok", fontWeight = FontWeight.Bold, color = MColors.Ink900)
                                TextButton(onClick = {
                                    preselectedChatIdForMemory = null
                                    showCreateMemorySheet = true
                                }) {
                                    Text("İlk anıyı sen ekle 📸", color = MColors.BrandDeep)
                                }
                            }
                        }
                    }
                } else {
                    // Öne Çıkan Büyük Vitrin Karesi (Hero Explore Card)
                    val heroPost = displayedMemories.first()
                    item(key = "hero_${heroPost.id}") {
                        HeroMemoryExploreCard(
                            post = heroPost,
                            onClick = { selectedMemoryForDetail = heroPost },
                            onLike = { onToggleLikeMemory(heroPost.id) }
                        )
                    }

                    // 2 Sütunlu Instagram Keşfet Izgarası (Birebir Aynı Boyutta Kareler)
                    val gridPosts = displayedMemories.drop(1)
                    items(gridPosts.chunked(2)) { rowPosts ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowPosts.forEach { post ->
                                Box(modifier = Modifier.weight(1f)) {
                                    GridMemoryExploreTile(
                                        post = post,
                                        onClick = { selectedMemoryForDetail = post },
                                        onLike = { onToggleLikeMemory(post.id) }
                                    )
                                }
                            }
                            if (rowPosts.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
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
                    id = "chat_img_${kotlin.random.Random.nextInt(10000, 99999)}",
                    authorId = DemoData.ME,
                    authorName = currentUser.name,
                    authorInitials = currentUser.initials,
                    sport = Sport.FOOTBALL,
                    venue = "",
                    area = currentDistrict,
                    city = currentCity,
                    distanceKm = 0f,
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
        LaunchedEffect(active.id, active.messages.size) {
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
                    ChatAvatar(active, compact = true)
                    Spacer(Modifier.width(10.dp))
                    Column(
                        modifier = Modifier.weight(1f).clickable { showInfo = true },
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
                            text = if (active.type == ThreadType.DIRECT) "Kişisel sohbet" else "${active.memberCount} kişi · ${active.type.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MColors.Ink500
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
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Bugün", style = MaterialTheme.typography.labelMedium, color = MColors.Ink400)
                    }
                }
                items(active.messages, key = { it.id }) { message ->
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
                                modifier = Modifier.widthIn(max = 296.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (!message.isMine && active.type != ThreadType.DIRECT) {
                                        Text(
                                            text = message.senderName,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MColors.BrandDeep
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
                                        text = message.time,
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
                    if (active.locked) {
                        Text(
                            text = "Mesaj göndermek için kadroya katıl.",
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
                                        onSendMessage(active.id, text)
                                        input = ""
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
            AlertDialog(
                shape = RoundedCornerShape(24.dp),
                containerColor = Color(0xFFFAFBFC),
                tonalElevation = 0.dp,
                onDismissRequest = { showInfo = false },
                icon = { ChatAvatar(active) },
                title = { Text(active.title) },
                text = { Text(if (active.locked) "Bu sohbet yalnızca katılımcılara açık." else "${active.type.label} · ${active.memberCount} kişi") },
                confirmButton = { TextButton(onClick = { showInfo = false }) { Text("Tamam") } }
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MAÇ SONRASI ANI BIRAK / SOHBETTE FOTOĞRAF PAYLAŞ SHEET
    // ═══════════════════════════════════════════════════════════════════════════
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
        val livePost = memories.firstOrNull { it.id == detailPost.id } ?: detailPost
        MemoryDetailSheet(
            post = livePost,
            threads = threads.filter { !it.locked },
            onDismiss = { selectedMemoryForDetail = null },
            onLike = { onToggleLikeMemory(livePost.id) },
            onShareToThread = { threadId ->
                onSendMemoryToChat(threadId, livePost.caption, livePost)
                selectedMemoryForDetail = null
                open(threadId)
            }
        )
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
                    text = "📍 ${post.area}",
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
                                .background(post.sceneTheme.accentColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = post.authorInitials,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MColors.Ink900
                            )
                        }
                        Text(
                            text = "${post.authorName} · ${post.timeAgo}",
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
            .height(184.dp)
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
                        text = "📍 ${post.area}",
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
    onPublish: (MatchMemoryPost, String?) -> Unit
) {
    var selectedSport by remember { mutableStateOf(Sport.FOOTBALL) }
    val venueOptions = listOf(
        "Moda Sahil Arena · Moda",
        "Kalamış Tenis Kulübü · Kalamış",
        "Caddebostan Sahil Kortu · Caddebostan",
        "Suadiye Padel Loft · Suadiye",
        "Fenerbahçe Kum Kortu · Fenerbahçe",
        "Acıbadem Kampüs Sahası · Acıbadem"
    )
    var selectedVenue by remember { mutableStateOf(venueOptions.first()) }
    var selectedTheme by remember { mutableStateOf(MemorySceneTheme.NIGHT_TURF) }
    var pickedBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var caption by remember { mutableStateOf("") }
    var scoreTag by remember { mutableStateOf("") }

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
                    title = "Saha Anısı Bırak",
                    subtitle = "Saha karesini konuma göre vitrine veya sohbete ekle",
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
                    area = selectedVenue.substringAfter(" · ", currentDistrict),
                    city = currentCity,
                    distanceKm = 0.5f,
                    caption = caption.ifBlank { "Saha karesi 🔥" },
                    matchScoreTag = scoreTag.takeIf { it.isNotBlank() },
                    timeAgo = "Şimdi",
                    likes = 1,
                    sceneTheme = selectedTheme,
                    customBitmap = pickedBitmap
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
                            onSelect = {
                                selectedTheme = it
                                pickedBitmap = null
                            },
                            icon = Icons.Rounded.Palette
                        )

                        MFloatingDropdownBox(
                            label = "Branş",
                            selectedValue = selectedSport,
                            options = listOf(Sport.FOOTBALL, Sport.TENNIS, Sport.PADEL, Sport.BASKETBALL, Sport.VOLLEYBALL),
                            optionLabel = { "${it.emoji} ${it.label}" },
                            onSelect = { selectedSport = it },
                            icon = Icons.Rounded.SportsSoccer
                        )

                        MFloatingDropdownBox(
                            label = "Konum & Saha",
                            selectedValue = selectedVenue,
                            options = venueOptions,
                            optionLabel = { "📍 $it" },
                            onSelect = { selectedVenue = it },
                            icon = Icons.Rounded.LocationOn
                        )

                        MFloatingDropdownBox(
                            label = "Paylaşım Yeri",
                            selectedValue = selectedShareTarget,
                            options = shareTargetOptions,
                            optionLabel = { it.second },
                            onSelect = { selectedShareTarget = it },
                            icon = Icons.Rounded.Share
                        )

                        MFloatingInputBox(
                            value = caption,
                            onValueChange = { caption = it },
                            label = "Anı Notu",
                            placeholder = "Bu kareden bir not bırak…",
                            icon = Icons.Rounded.EditNote,
                            singleLine = false,
                            minLines = 2,
                            maxLines = 3
                        )

                        MFloatingInputBox(
                            value = scoreTag,
                            onValueChange = { scoreTag = it },
                            label = "Etiket / Skor (İsteğe Bağlı)",
                            placeholder = "Maç Öncesi · Isınma · 7 - 5",
                            icon = Icons.Rounded.EmojiEvents,
                            singleLine = true
                        )

                        MButton(
                            text = "Anıyı Paylaş",
                            onClick = {
                                val post = MatchMemoryPost(
                                    id = "mem_${kotlin.random.Random.nextInt(10000, 99999)}",
                                    authorId = DemoData.ME,
                                    authorName = currentUser.name,
                                    authorInitials = currentUser.initials,
                                    sport = selectedSport,
                                    venue = selectedVenue.substringBefore(" · "),
                                    area = selectedVenue.substringAfter(" · ", currentDistrict),
                                    city = currentCity,
                                    distanceKm = 0.4f,
                                    caption = caption.trim().ifBlank { "Sahadan güzel bir anı! 🔥" },
                                    matchScoreTag = scoreTag.trim().takeIf { it.isNotBlank() },
                                    timeAgo = "Şimdi",
                                    likes = 1,
                                    likedByMe = true,
                                    sceneTheme = selectedTheme,
                                    customBitmap = pickedBitmap
                                )
                                onPublish(post, selectedShareTarget.first)
                            },
                            icon = Icons.Rounded.Check
                        )
                    }
                }
            }
        }
    }
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
    onLike: () -> Unit,
    onShareToThread: (String) -> Unit
) {
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
                        Surface(
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
                }
            }
        }
    }
}

@Composable
private fun ChatAvatar(thread: ChatThread, compact: Boolean = false) {
    Box(
        modifier = Modifier
            .size(if (compact) 40.dp else 52.dp)
            .clip(if (thread.type == ThreadType.DIRECT) CircleShape else RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    if (thread.type == ThreadType.DIRECT) listOf(Color(0xFFFFE0C7), MColors.BrandTint)
                    else listOf(Color(0xFFDCD6FF), Color(0xFFEDF0FF))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (thread.type == ThreadType.DIRECT) {
            Text(
                text = thread.title.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString(""),
                style = MaterialTheme.typography.titleMedium,
                color = MColors.BrandDeep
            )
        } else {
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
