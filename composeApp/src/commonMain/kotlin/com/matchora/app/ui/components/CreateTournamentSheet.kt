package com.matchora.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchora.app.data.*
import com.matchora.app.domain.Sport
import com.matchora.app.state.*
import com.matchora.app.ui.theme.MColors

// ═══════════════════════════════════════════════════════════════════════════════
// CREATE TOURNAMENT & E-SPORTS CUP SHEET
// Kullanıcıların hem fiziksel hem E-Spor oyunları için turnuva açmasını sağlar
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTournamentSheet(
    onDismiss: () -> Unit,
    onCreateTournament: (TournamentInfo) -> Unit,
    tokenBalance: Int = Int.MAX_VALUE,
    organizerName: String = "Matchora Topluluk",
    newTournamentId: () -> String = { "tr_${(1000..9999).random()}" },
    onRequestTokens: () -> Unit = {},
    currentCity: String = "İstanbul", currentArea: String = "Kadıköy",
    venues: List<VenueInfo> = emptyList(),
    primarySport: Sport = Sport.FOOTBALL,
    onPublished: (String) -> Unit = {}
) {
    val form = rememberCreationForm("tournament")
    val draft = form.restored
    val backend = LocalBackendRepository.current
    val liveMode = backend?.enabled == true
    val config = backend?.configurations?.collectAsState()?.value
    val lockAmount = if (liveMode) config?.o("fees")?.i("tournament", 20) ?: 20 else 20
    var formatCode by remember(form) { mutableStateOf(draft.s("formatCode", "KNOCKOUT")) }
    var liveCapacity by remember(form) { mutableIntStateOf(draft.i("liveCapacity", 16)) }
    var minRating by remember(form) { mutableIntStateOf(draft.i("minRating", 0)) }
    var verifiedOnly by remember(form) { mutableStateOf(draft.b("verifiedOnly")) }
    var tournamentCategory by remember(form) { mutableStateOf(draft.formEnum("category", primarySport.category)) }
    var selectedSport by remember(form) { mutableStateOf(draft.formEnum("sport", primarySport)) }
    var rosterSize by remember(form, selectedSport) {
        val defaultSize = when (selectedSport) { Sport.FOOTBALL, Sport.BASKETBALL -> 5; Sport.VOLLEYBALL -> 6; Sport.PADEL -> 2; else -> 1 }
        mutableIntStateOf(if (selectedSport.name == draft.s("sport", primarySport.name)) draft.i("rosterSize", defaultSize) else defaultSize)
    }
    var customSportName by remember(form) { mutableStateOf(draft.s("customSportName", "")) }
    var customIsGaming by remember(form) { mutableStateOf(draft.b("customIsGaming")) }
    var title by remember(form) { mutableStateOf(draft.s("title", "")) }
    var dates by remember(form) { mutableStateOf(draft.s("dates", "")) }
    var organizer by remember(form) { mutableStateOf(draft.s("organizer", organizerName)) }
    var prizeToken by remember(form) { mutableStateOf(draft.s("prizeToken", "")) }
    var prizeCash by remember(form) { mutableStateOf(draft.s("prizeCash", "")) }
    var requirement by remember(form) { mutableStateOf(draft.s("requirement", "")) }
    var tournamentCity by remember(form) { mutableStateOf(draft.s("tournamentCity", currentCity)) }
    var tournamentArea by remember(form) { mutableStateOf(draft.s("tournamentArea", currentArea)) }
    var tournamentVenue by remember(form) { mutableStateOf(draft.s("tournamentVenue", "")) }
    var tournamentNetworkScope by remember(form) { mutableStateOf(draft.formEnum("scope", EventNetworkScope.CITY)) }
    var showRegion by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }

    // E-Spor / Gaming Özellikleri
    var platform by remember(form) { mutableStateOf(draft.s("platform", "PS5 & PC Crossplay")) }
    var format by remember(form) { mutableStateOf(draft.s("format", "16 Takım · Tek Eleme")) }
    var formatStyle by remember(form) { mutableStateOf(draft.s("formatStyle", "Tek Eleme")) }
    var serverRegion by remember(form) { mutableStateOf(draft.s("serverRegion", "TR Sunucusu · Discord Lobi")) }

    val isGaming = (selectedSport.isESport || selectedSport == Sport.CHESS) || (selectedSport == Sport.CUSTOM && customIsGaming)

    CreationDraftEffect(form) { apiPayload(
        "formatCode" to formatCode, "customSportName" to customSportName, "title" to title,
        "dates" to dates, "organizer" to organizer, "prizeToken" to prizeToken,
        "prizeCash" to prizeCash, "requirement" to requirement, "tournamentCity" to tournamentCity,
        "tournamentArea" to tournamentArea, "tournamentVenue" to tournamentVenue, "platform" to platform,
        "format" to format, "formatStyle" to formatStyle, "serverRegion" to serverRegion,
        "liveCapacity" to liveCapacity, "minRating" to minRating, "verifiedOnly" to verifiedOnly,
        "customIsGaming" to customIsGaming, "rosterSize" to rosterSize, "category" to tournamentCategory,
        "sport" to selectedSport, "scope" to tournamentNetworkScope
    ) }

    ModalBottomSheet(
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 0.dp,
        scrimColor = Color(0xFF0B1220).copy(alpha = .48f),
        onDismissRequest = { form.requestDismiss(onDismiss) },
        modifier = Modifier.statusBarsPadding().padding(top = 12.dp),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { it != SheetValue.Hidden || form.confirmDismiss(onDismiss) }),
        containerColor = Color(0xFFF8FAFC),
        dragHandle = { MPremiumSheetHandle() }
    ) {
        CreationFormBody(form) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                MSheetHeading("Turnuva oluştur", "Rekabeti bir üst seviyeye taşı.", Icons.Rounded.EmojiEvents, { form.requestDismiss(onDismiss) })
            }

            // Bölüm 1: Turnuva Kimliği & Branş
            item {
                TournamentSectionCard {
                    TournamentPreviewBanner(
                        title = title.ifBlank { "Yeni Turnuva" },
                        sportEmoji = selectedSport.emoji,
                        sportLabel = if (selectedSport == Sport.CUSTOM && customSportName.isNotBlank()) customSportName else selectedSport.label,
                        capacity = liveCapacity,
                        formatStyle = formatStyle,
                        lockAmount = lockAmount
                    )

                    TournamentFieldLabel("TURNUVA KATEGORİSİ")
                    MSegmentPillBar(
                        options = listOf(
                            MSegmentPillOption(SportCategory.PHYSICAL, "Fiziksel", emoji = "🏟️"),
                            MSegmentPillOption(SportCategory.ESPORTS, "E-Spor", emoji = "🎮"),
                            MSegmentPillOption(SportCategory.CUSTOM, "Özel Branş", emoji = "✨")
                        ),
                        selectedValue = tournamentCategory,
                        onSelect = { cat ->
                            tournamentCategory = cat
                            if (cat == SportCategory.PHYSICAL && selectedSport.category != SportCategory.PHYSICAL) {
                                selectedSport = Sport.FOOTBALL
                                title = "$tournamentArea Halı Saha Kupası"
                                format = "$liveCapacity Takım · Tek Eleme"
                            } else if (cat == SportCategory.ESPORTS && selectedSport.category != SportCategory.ESPORTS) {
                                selectedSport = Sport.ESPORTS_FC
                                title = "FC 25 Kadıköy E-Kupa"
                                platform = "PS5 & PC Crossplay"
                                format = "$liveCapacity Takım · 1v1 Tek Maç Eleme"
                            } else if (cat == SportCategory.CUSTOM) {
                                selectedSport = Sport.CUSTOM
                                title = "${customSportName.ifBlank { "Özel Branş" }} Kadıköy Kupası"
                            }
                        }
                    )

                    if (tournamentCategory == SportCategory.CUSTOM || selectedSport == Sport.CUSTOM) {
                        MFloatingInputBox(
                            label = "Özel Turnuva Branşı / Oyun Adı",
                            value = customSportName,
                            onValueChange = { customSportName = it },
                            icon = Icons.Rounded.AutoAwesome
                        )
                        TournamentFieldLabel("TURNUVA ORTAMI")
                        MSegmentPillBar(
                            options = listOf(
                                MSegmentPillOption(false, "Fiziksel Saha", emoji = "🏟️"),
                                MSegmentPillOption(true, "Online E-Spor", emoji = "🎮")
                            ),
                            selectedValue = customIsGaming,
                            onSelect = { customIsGaming = it }
                        )
                    } else {
                        val currentList = if (tournamentCategory == SportCategory.PHYSICAL) physicalSports else esportSports
                        SportPickerField(
                            selectedSport = selectedSport,
                            onSelect = { sport ->
                                if (sport != null) {
                                    selectedSport = sport
                                    if (sport == Sport.ESPORTS_FC) {
                                        platform = "PS5 & PC Crossplay"
                                        format = "$liveCapacity Takım · 1v1 Tek Maç Eleme"
                                    } else if (sport == Sport.ESPORTS_VALORANT) {
                                        platform = "PC"
                                        format = "$liveCapacity Takım · 5v5 Bracket"
                                    } else if (sport == Sport.ESPORTS_LOL) {
                                        platform = "PC"
                                        format = "$liveCapacity Takım · 5v5 Grup Aşaması"
                                    } else if (sport == Sport.CHESS) {
                                        platform = "Yüz Yüze & Chess.com"
                                        format = "$liveCapacity Sporcu · İsviçre Sistemi"
                                    } else {
                                        format = "$liveCapacity Takım · $formatStyle"
                                    }
                                }
                            },
                            sports = currentList,
                            allowAll = false
                        )
                    }

                    MFloatingInputBox(
                        label = "Turnuva Adı",
                        value = title,
                        onValueChange = { title = it },
                        placeholder = "Örn: FC 25 $tournamentCity E-Kupa",
                        subtitle = "Resmi kupa veya topluluk ligi başlığı",
                        icon = Icons.Rounded.EmojiEvents,
                        badgeText = "Zorunlu",
                        suggestions = listOf(
                            "${selectedSport.label} $tournamentArea Kupası",
                            "${selectedSport.label} Hafta Sonu Ligi",
                            "$tournamentCity Şampiyonluk Turnuvası"
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MPremiumTextField(
                            value = dates,
                            onValueChange = { dates = it },
                            label = "Tarih Aralığı",
                            placeholder = "25 - 28 Eki",
                            modifier = Modifier.weight(1f)
                        )
                        MPremiumTextField(
                            value = organizer,
                            onValueChange = { organizer = it },
                            label = "Organizatör",
                            placeholder = "Kulüp / İsim",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Bölüm 2: Format, Kapasite & Saha / Platform
            item {
                TournamentSectionCard {
                    TournamentSectionHeader(Icons.Rounded.AccountTree, "Format ve Organizasyon")

                    TournamentFieldLabel("TURNUVA USULÜ")
                    MSegmentPillBar(
                        options = listOf(
                            MSegmentPillOption("KNOCKOUT", "Tek Eleme", icon = Icons.Rounded.AccountTree),
                            MSegmentPillOption("ROUND_ROBIN", "Lig Usulü", icon = Icons.Rounded.FormatListNumbered)
                        ),
                        selectedValue = formatCode,
                        onSelect = { code ->
                            formatCode = code
                            formatStyle = if (code == "ROUND_ROBIN") "Lig Usulü" else "Tek Eleme"
                            if (code == "ROUND_ROBIN" && liveCapacity > 16) {
                                liveCapacity = 16
                            } else if (code == "KNOCKOUT" && liveCapacity !in listOf(4, 8, 16, 32)) {
                                liveCapacity = 8
                            }
                            format = "$liveCapacity Takım · $formatStyle"
                        }
                    )

                    val capacities = if (formatCode == "ROUND_ROBIN") listOf(4, 6, 8, 10, 12, 16) else listOf(4, 8, 16, 32)
                    TournamentFieldLabel("TAKIM KAPASİTESİ")
                    TournamentCapacitySelector(
                        capacities = capacities,
                        selectedCapacity = liveCapacity,
                        onSelect = { cap ->
                            liveCapacity = cap
                            format = "$cap Takım · $formatStyle"
                        }
                    )

                    MFloatingDropdownBox(
                        label = "Maç kadrosu (takım başı)",
                        selectedValue = rosterSize,
                        options = (1..25).toList(),
                        optionLabel = { "$it sporcu" },
                        onSelect = { rosterSize = it },
                        icon = Icons.Rounded.Groups
                    )
                    Text(
                        text = "Kaptan kayıtta bu sayıda oyuncu seçer. Takımdaki diğer sporcular kulüpte kalır.",
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = MColors.Ink500
                    )

                    TournamentFieldLabel("TURNUVA KAPSAMI")
                    MSegmentPillBar(
                        options = EventNetworkScope.entries.map { MSegmentPillOption(it, it.label) },
                        selectedValue = tournamentNetworkScope,
                        onSelect = { tournamentNetworkScope = it },
                        fontSizeSp = 11f
                    )

                    MFloatingActionRow(
                        label = "Turnuva bölgesi",
                        valueText = "$tournamentCity · $tournamentArea",
                        icon = Icons.Rounded.LocationOn,
                        onClick = { showRegion = true }
                    )

                    if (!isGaming) {
                        VenueChoiceField(
                            label = "Turnuva Tesisi / Sahası",
                            value = tournamentVenue,
                            onChange = { tournamentVenue = it.take(160) },
                            previewOptions = venues.filter { matchesTurkishPlace(it.city, tournamentCity) }.map { "${it.name} · ${it.area}" }.distinct(),
                            city = tournamentCity
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MPremiumTextField(
                                value = platform,
                                onValueChange = { platform = it },
                                label = "Oyun Platformu",
                                placeholder = "PS5 / PC / Crossplay",
                                modifier = Modifier.weight(1f)
                            )
                            MPremiumTextField(
                                value = serverRegion,
                                onValueChange = { serverRegion = it },
                                label = "Sunucu / Lobi",
                                placeholder = "TR Sunucusu · Discord",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Bölüm 3: Katılım Şartları & Ödül Havuzu
            item {
                TournamentSectionCard {
                    TournamentSectionHeader(Icons.Rounded.EmojiEvents, "Ödül Havuzu ve Katılım Kriteri")

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MPremiumTextField(
                            value = prizeToken,
                            onValueChange = { prizeToken = it },
                            label = "Token Ödülü",
                            placeholder = "500 TKN",
                            modifier = Modifier.weight(1f)
                        )
                        MPremiumTextField(
                            value = prizeCash,
                            onValueChange = { prizeCash = it },
                            label = "Sponsor / Ek Ödül",
                            placeholder = "Kupa & Madalya",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    TournamentFieldLabel("MİNİMUM MMR SINIRI")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf(0, 1000, 1200, 1500, 1800, 2000)) { mmr ->
                            MChip(
                                text = if (mmr == 0) "MMR Sınırı Yok" else "$mmr+ MMR",
                                selected = minRating == mmr,
                                onClick = { minRating = mmr }
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.VerifiedUser,
                                contentDescription = null,
                                tint = MColors.BrandDeep,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Telefonu doğrulanmış kadro",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MColors.Ink900,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = verifiedOnly,
                                onCheckedChange = { verifiedOnly = it }
                            )
                        }
                    }

                    MFloatingInputBox(
                        label = "Katılım Şartı & Kısa Açıklama",
                        value = requirement,
                        onValueChange = { requirement = it },
                        placeholder = "1.500+ MMR veya Açık Katılım",
                        subtitle = "Koşullar kayıtta ve fikstürde denetlenir. Ödül notu otomatik ödeme oluşturmaz.",
                        icon = Icons.Rounded.Gavel,
                        badgeText = "Kural & Not",
                        suggestions = listOf(
                            "1.500+ MMR · Doğrulanmış Profil",
                            "Açık Katılım · Tüm Seviyeler",
                            "Lisanslı Kulüp Kadroları"
                        )
                    )
                }
            }

            // Turnuvayı Başlat Butonu
            item {
                CreationFormNotice(form)
                formError?.let { error ->
                    Text(
                        text = error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MColors.Rose,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                MButton(
                    text = if (form.busy) "Turnuvan yayınlanıyor…" else if (tokenBalance < lockAmount && !form.hasAttempt) "Token yükle · $lockAmount TKN gerekli" else "Turnuvayı yayınla · $lockAmount TKN kilitle",
                    onClick = {
                        val validationError = when {
                            title.isBlank() -> "Turnuva adını gir."
                            organizer.isBlank() -> "Organizatör adını gir."
                            selectedSport == Sport.CUSTOM && customSportName.isBlank() -> "Özel branş adını gir."
                            dates.isBlank() -> "Turnuva tarihini gir."
                            !liveMode && prizeToken.isBlank() && prizeCash.isBlank() -> "Ödül havuzunu belirt."
                            else -> null
                        }
                        if (validationError != null) {
                            formError = validationError
                            return@MButton
                        }
                        formError = null
                        if (tokenBalance < lockAmount && !form.hasAttempt) {
                            onRequestTokens()
                            return@MButton
                        }
                        val newTournament = TournamentInfo(
                            id = newTournamentId(),
                            name = title.trim(),
                            sport = selectedSport,
                            format = "$liveCapacity Takım · $formatStyle".take(80),
                            participants = 0,
                            capacity = liveCapacity,
                            dates = dates,
                            organizer = organizer.trim(),
                            requirement = requirement,
                            status = "Kayıtlar Açık",
                            feeNote = if (liveMode && prizeToken.isBlank() && prizeCash.isBlank()) "Katılım ücretsiz · Ödül tanımlanmadı" else "Katılım ücretsiz · Organizatörün ödül notu: ${prizeToken.ifBlank { "—" }} + ${prizeCash.ifBlank { "—" }}",
                            bracket = emptyList(),
                            standings = emptyList(),
                            isLeague = formatCode == "ROUND_ROBIN",
                            customSportName = if (selectedSport == Sport.CUSTOM) customSportName.trim() else null,
                            creatorId = DemoData.ME,
                            organizerLockTokens = lockAmount,
                            formatCode = formatCode, minRating = minRating, verifiedOnly = verifiedOnly, rosterSize = rosterSize,
                            city = tournamentCity, area = tournamentArea,
                            networkScope = tournamentNetworkScope,
                            venue = tournamentVenue.trim(),
                            prizeToken = prizeToken.trim(),
                            prizeCash = prizeCash.trim(),
                            gamePlatform = if (isGaming) platform.trim() else "",
                            serverRegion = if (isGaming) serverRegion.trim() else ""
                        )
                        form.submit("tournament.create", newTournament.toApiPayload(), { onCreateTournament(newTournament) }, onPublished, onDismiss)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Rounded.EmojiEvents,
                    enabled = !form.busy
                )
            }
        }
    }
    }
    CreationFormDialogs(form, onDismiss)
    if (showRegion) CityAndLocationSelectorSheet(tournamentCity, tournamentArea, false, { showRegion = false }, { city, area, _ -> tournamentCity = city; tournamentArea = area })
}

@Composable
private fun TournamentSectionCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
            content = content
        )
    }
}

@Composable
private fun TournamentPreviewBanner(
    title: String,
    sportEmoji: String,
    sportLabel: String,
    capacity: Int,
    formatStyle: String,
    lockAmount: Int
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF162032),
        border = BorderStroke(1.dp, Color(0xFF2B3B54)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFF1F2D42), Color(0xFF162032))))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFFFD48A).copy(alpha = 0.14f), RoundedCornerShape(11.dp))
                    .border(1.dp, Color(0xFFFFD48A).copy(alpha = 0.30f), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.EmojiEvents, contentDescription = null, modifier = Modifier.size(21.dp), tint = Color(0xFFFFD48A))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$sportEmoji $sportLabel · $capacity Takım · $formatStyle",
                    fontSize = 11.5.sp,
                    color = Color(0xFFCBD5E1),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                shape = RoundedCornerShape(9.dp),
                color = Color(0xFFFFD48A).copy(alpha = 0.16f),
                border = BorderStroke(1.dp, Color(0xFFFFD48A).copy(alpha = 0.35f))
            ) {
                Text(
                    text = "$lockAmount TKN Kilit",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFFD48A),
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun TournamentSectionHeader(icon: ImageVector, title: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(9.dp),
            color = Color(0xFFFFF3EC),
            border = BorderStroke(1.dp, MColors.Brand.copy(alpha = 0.2f)),
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MColors.BrandDeep, modifier = Modifier.size(15.dp))
            }
        }
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MColors.Ink900
        )
    }
}

@Composable
private fun TournamentFieldLabel(text: String) {
    Text(
        text = text,
        fontSize = 10.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        color = MColors.Ink500
    )
}

@Composable
private fun TournamentCapacitySelector(
    capacities: List<Int>,
    selectedCapacity: Int,
    onSelect: (Int) -> Unit
) {
    if (capacities.size <= 4) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            capacities.forEach { cap ->
                CapacityPill(
                    label = "$cap Takım",
                    selected = selectedCapacity == cap,
                    onClick = { onSelect(cap) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    } else {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(capacities) { cap ->
                CapacityPill(
                    label = "$cap Takım",
                    selected = selectedCapacity == cap,
                    onClick = { onSelect(cap) }
                )
            }
        }
    }
}

@Composable
private fun CapacityPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MColors.Ink900 else Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, if (selected) MColors.Ink900 else Color(0xFFE2E8F0)),
        modifier = modifier.height(36.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else MColors.Ink700,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
