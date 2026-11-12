package com.matchora.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    primarySport: Sport = Sport.FOOTBALL
) {
    val backend = LocalBackendRepository.current
    val liveMode = backend?.enabled == true
    val config = backend?.configurations?.collectAsState()?.value
    val lockAmount = if (liveMode) config?.o("fees")?.i("tournament", 20) ?: 20 else 20
    var formatCode by remember { mutableStateOf("KNOCKOUT") }
    var liveCapacity by remember { mutableIntStateOf(16) }
    var minRating by remember { mutableIntStateOf(0) }
    var verifiedOnly by remember { mutableStateOf(false) }
    val supportedFormats = listOf(2, 4, 8, 16, 32).map { "KNOCKOUT" to it } + listOf(2, 4, 6, 8, 10, 12, 16).map { "ROUND_ROBIN" to it }
    fun formatLabel(option: Pair<String, Int>) = "${option.second} takım · ${if (option.first == "ROUND_ROBIN") "Lig" else "Tek eleme"}"
    var tournamentCategory by remember { mutableStateOf(primarySport.category) }
    var selectedSport by remember { mutableStateOf(primarySport) }
    var rosterSize by remember(selectedSport) { mutableIntStateOf(when (selectedSport) { Sport.FOOTBALL, Sport.BASKETBALL -> 5; Sport.VOLLEYBALL -> 6; Sport.PADEL -> 2; else -> 1 }) }
    var customSportName by remember { mutableStateOf("") }
    var customIsGaming by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var dates by remember { mutableStateOf("") }
    var organizer by remember { mutableStateOf(organizerName) }
    var prizeToken by remember { mutableStateOf("") }
    var prizeCash by remember { mutableStateOf("") }
    var requirement by remember { mutableStateOf("") }
    var tournamentCity by remember { mutableStateOf(currentCity) }
    var tournamentArea by remember { mutableStateOf(currentArea) }
    var tournamentVenue by remember { mutableStateOf("") }
    var tournamentNetworkScope by remember { mutableStateOf(EventNetworkScope.CITY) }
    var showRegion by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }

    // E-Spor / Gaming Özellikleri
    var platform by remember { mutableStateOf("PS5 & PC Crossplay") }
    var format by remember { mutableStateOf("16 Takım · Tek Eleme") }
    var formatStyle by remember { mutableStateOf("Tek Eleme") }
    var serverRegion by remember { mutableStateOf("TR Sunucusu · Discord Lobi") }

    val isGaming = (selectedSport.isESport || selectedSport == Sport.CHESS) || (selectedSport == Sport.CUSTOM && customIsGaming)

    ModalBottomSheet(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 0.dp,
        scrimColor = androidx.compose.ui.graphics.Color(0xFF0B1220).copy(alpha = .48f),

        onDismissRequest = onDismiss,
        modifier = Modifier.statusBarsPadding().padding(top = 12.dp),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFFFAFAFA),
        dragHandle = { MPremiumSheetHandle() }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Başlık
            item {
                MSheetHeading("Turnuva oluştur", "Rekabeti bir üst seviyeye taşı.", Icons.Rounded.EmojiEvents, onDismiss)
            }

            item {
                Surface(shape = RoundedCornerShape(24.dp), color = Color(0xFF20283A)) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(Modifier.size(58.dp).background(Color(0xFFFFD48A).copy(alpha = .16f), RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.EmojiEvents, null, Modifier.size(32.dp), tint = Color(0xFFFFD48A))
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("CANLI KUPA ÖNİZLEMESİ", fontSize = 10.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD48A))
                            Text(title.ifBlank { "Turnuvanın adı" }, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("${selectedSport.emoji} ${selectedSport.label} · $dates", fontSize = 12.sp, color = Color(0xFFD2D8E2))
                        }
                    }
                }
            }

            // Kategori & Branş Seçimi
            item {
                Text(
                    text = "TURNUVA KATEGORİSİ",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MColors.Ink500
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Kategori Butonları: [🎮 E-Spor] [🏟️ Fiziksel] [✨ Özel Branş] — Light Luxury Segmented Control
                Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = Color(0xFFF1F5F9), // Light Luxury Segmented Track
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            Triple(SportCategory.ESPORTS, "🎮", "E-Spor"),
                            Triple(SportCategory.PHYSICAL, "🏟️", "Fiziksel"),
                            Triple(SportCategory.CUSTOM, "✨", "Özel Branş")
                        ).forEach { (cat, icon, label) ->
                            val isSel = tournamentCategory == cat
                            val animatedBg by animateColorAsState(
                                targetValue = if (isSel) Color.White else Color.Transparent,
                                animationSpec = tween(180)
                            )
                            val animatedTextColor by animateColorAsState(
                                targetValue = if (isSel) MColors.Brand else Color(0xFF64748B),
                                animationSpec = tween(180)
                            )

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = animatedBg,
                                border = if (isSel) BorderStroke(1.dp, Color(0x18000000)) else null,
                                shadowElevation = if (isSel) 2.dp else 0.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clickable {
                                        tournamentCategory = cat
                                        if (cat == SportCategory.PHYSICAL && selectedSport.category != SportCategory.PHYSICAL) {
                                            selectedSport = Sport.FOOTBALL
                                            title = "Kadıköy Halı Saha Sonbahar Kupası"
                                            format = "16 Takım · Tek Eleme"
                                        } else if (cat == SportCategory.ESPORTS && selectedSport.category != SportCategory.ESPORTS) {
                                            selectedSport = Sport.ESPORTS_FC
                                            title = "FC 25 Kadıköy E-Kupa"
                                            platform = "PS5 & PC Crossplay"
                                            format = "16 Takım · 1v1 Tek Maç Eleme"
                                        } else if (cat == SportCategory.CUSTOM) {
                                            selectedSport = Sport.CUSTOM
                                            title = "${customSportName.ifBlank { "Özel Branş" }} Kadıköy Kupası"
                                        }
                                    }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(icon, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.SemiBold,
                                            color = animatedTextColor,
                                            fontSize = 11.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (tournamentCategory == SportCategory.CUSTOM || selectedSport == Sport.CUSTOM) {
                    MFloatingInputBox(
                        label = "Özel Turnuva Branşı / Oyun Adı",
                        value = customSportName,
                        onValueChange = {
                            customSportName = it
                        },
                        icon = Icons.Rounded.AutoAwesome
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    MFloatingDropdownBox(
                        label = "Turnuva Ortamı",
                        selectedValue = customIsGaming,
                        options = listOf(false, true),
                        optionLabel = { if (it) "🎮 Online E-Spor" else "🏟️ Fiziksel Saha / Salon" },
                        onSelect = { customIsGaming = it },
                        icon = Icons.Rounded.Category
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
                                    format = "16 Takım · 1v1 Tek Maç Eleme"
                                } else if (sport == Sport.ESPORTS_VALORANT) {
                                    platform = "PC"
                                    format = "8 Takım · 5v5 Bracket"
                                } else if (sport == Sport.ESPORTS_LOL) {
                                    platform = "PC"
                                    format = "8 Takım · 5v5 Grup Aşaması"
                                } else if (sport == Sport.CHESS) {
                                    platform = "Yüz Yüze & Chess.com"
                                    format = "32 Sporcu · 7 Tur İsviçre Sistemi"
                                } else {
                                    format = "16 Takım · Eleme Usulü"
                                }
                            }
                        },
                        sports = currentList,
                        allowAll = false
                    )
                }
            }

            // Turnuva Adı & Tarihleri
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MFloatingInputBox(
                        label = "Turnuva Adı",
                        value = title,
                        onValueChange = { title = it },
                        placeholder = "Örn: FC 25 Kadıköy E-Kupa",
                        subtitle = "Resmi kupa veya topluluk ligi başlığı",
                        icon = Icons.Rounded.EmojiEvents,
                        badgeText = "Zorunlu",
                        suggestions = listOf(
                            "${selectedSport.label} Kadıköy Kupası",
                            "${selectedSport.label} Hafta Sonu Ligi",
                            "Şehir Şampiyonluk Turnuvası"
                        )
                    )

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        shadowElevation = 5.dp,
                        border = BorderStroke(1.dp, Color(0xFFE4E9F2)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                    value = dates,
                                    onValueChange = { dates = it },
                                    label = { Text("Tarih Aralığı") },
                                    placeholder = { Text("25 - 28 Eki") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f),
                                    colors = premiumTextFieldColors()
                                )

                                MFloatingDropdownBox(
                                    label = "Format / Kapasite", selectedValue = formatCode to liveCapacity,
                                    options = supportedFormats, optionLabel = ::formatLabel,
                                    onSelect = {
                                        formatCode = it.first
                                        liveCapacity = it.second
                                        formatStyle = if (it.first == "ROUND_ROBIN") "Lig Usulü" else "Tek Eleme"
                                    },
                                    modifier = Modifier.weight(1.2f)
                                )
                            }

                            MFloatingDropdownBox(
                                label = "Turnuva Usulü",
                                selectedValue = formatStyle,
                                options = listOf("Tek Eleme", "Lig Usulü", "Grup + Eleme", "İsviçre Sistemi"),
                                optionLabel = { it },
                                onSelect = {
                                    formatStyle = it
                                    formatCode = if (it == "Lig Usulü" || it == "İsviçre Sistemi") "ROUND_ROBIN" else "KNOCKOUT"
                                    if (formatCode == "ROUND_ROBIN" && liveCapacity > 16) liveCapacity = 16
                                },
                                icon = Icons.Rounded.AccountTree
                            )

                            MFloatingDropdownBox("Maç kadrosu · takım başına", rosterSize, (1..25).toList(), { "$it sporcu" }, { rosterSize = it }, icon = Icons.Rounded.Groups)
                            Text("Kaptan kayıtta bu sayıda oyuncu seçer. Takımdaki diğer sporcular kulüpte kalır; turnuva kadrosuna otomatik eklenmez.", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)

                            MFloatingInputBox(
                                label = "Organizatör Adı",
                                value = organizer,
                                onValueChange = { organizer = it },
                                placeholder = "Organizatör veya kulüp adı",
                                icon = Icons.Rounded.VerifiedUser
                            )

                            MFloatingDropdownBox(
                                label = "Turnuva Ağı / Kapsamı",
                                selectedValue = tournamentNetworkScope,
                                options = EventNetworkScope.entries,
                                optionLabel = { it.label },
                                onSelect = { tournamentNetworkScope = it },
                                icon = Icons.Rounded.Hub
                            )

                            if (!isGaming) {
                                VenueChoiceField(
                                    "Turnuva Tesisi / Sahası",
                                    tournamentVenue,
                                    { tournamentVenue = it.take(160) },
                                    emptyList(),
                                    tournamentCity
                                )
                            }

                            if (isGaming) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                        value = platform,
                                        onValueChange = { platform = it },
                                        label = { Text("Oyun Platformu") },
                                        placeholder = { Text("PS5 / PC / Crossplay") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f),
                                        colors = premiumTextFieldColors()
                                    )

                                    OutlinedTextField(
                                        textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                        value = serverRegion,
                                        onValueChange = { serverRegion = it },
                                        label = { Text("Sunucu / İletişim") },
                                        placeholder = { Text("TR Sunucusu · Discord") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f),
                                        colors = premiumTextFieldColors()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { MFloatingActionRow("Turnuva bölgesi", "$tournamentCity · $tournamentArea", Icons.Rounded.LocationOn, onClick = { showRegion = true }) }
            item {
                MSubCard {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        MFloatingDropdownBox("Her oyuncu için en düşük MMR", minRating,
                            listOf(0, 1000, 1200, 1500, 1800, 2000), { if (it == 0) "MMR sınırı yok" else "$it MMR" }, { minRating = it })
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Telefonu doğrulanmış kadro", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                            Switch(verifiedOnly, { verifiedOnly = it })
                        }
                        Text("Koşullar kayıtta ve fikstür oluşturulurken her oyuncu için denetlenir. Ödül notu otomatik ödeme oluşturmaz.", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
                    }
                }
            }

            // Ödül Havuzu & Şartlar
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        shadowElevation = 5.dp,
                        border = BorderStroke(1.dp, Color(0xFFE4E9F2)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Ödül havuzu",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MColors.Gold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                    value = prizeToken,
                                    onValueChange = { prizeToken = it },
                                    label = { Text("Token Ödülü") },
                                    placeholder = { Text("500 TKN") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f),
                                    colors = premiumTextFieldColors()
                                )

                                OutlinedTextField(
                                    textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                    value = prizeCash,
                                    onValueChange = { prizeCash = it },
                                    label = { Text("Ek Ödül") },
                                    placeholder = { Text("TKN 10.000") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f),
                                    colors = premiumTextFieldColors()
                                )
                            }
                        }
                    }

                    MFloatingInputBox(
                        label = "Katılım Şartı & Kısa Açıklama",
                        value = requirement,
                        onValueChange = { requirement = it },
                        placeholder = "1.500+ MMR veya Açık Katılım",
                        subtitle = "Turnuva kurallarını ve katılım kriterini özetle",
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
                formError?.let { error ->
                    Text(error, fontSize = 11.sp, color = MColors.Rose, modifier = Modifier.padding(bottom = 8.dp))
                }
                MButton(
                    text = if (tokenBalance < lockAmount) "Token yükle · $lockAmount TKN gerekli" else "Turnuvayı yayınla · $lockAmount TKN kilitle",
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
                        if (tokenBalance < lockAmount) {
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
                        onCreateTournament(newTournament)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Rounded.EmojiEvents,
                    enabled = true
                )
            }
        }
    }
    if (showRegion) CityAndLocationSelectorSheet(tournamentCity, tournamentArea, false, { showRegion = false }, { city, area, _ -> tournamentCity = city; tournamentArea = area })
}
