package com.matchora.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Sports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchora.app.domain.Sport
import com.matchora.app.state.*
import com.matchora.app.ui.theme.MColors

private val Sport.family: String
    get() = when (this) {
        Sport.TENNIS, Sport.PADEL, Sport.TABLE_TENNIS, Sport.BADMINTON, Sport.SQUASH, Sport.PICKLEBALL -> "Raket"
        Sport.BOXING, Sport.KICKBOXING, Sport.WRESTLING, Sport.JUDO, Sport.KARATE, Sport.TAEKWONDO, Sport.FENCING -> "Dövüş"
        Sport.SWIMMING, Sport.WATER_POLO, Sport.ROWING, Sport.CANOEING, Sport.SAILING, Sport.SURFING, Sport.DIVING -> "Su"
        Sport.SKIING, Sport.SNOWBOARDING, Sport.ICE_SKATING, Sport.ICE_HOCKEY -> "Kış"
        Sport.HIKING, Sport.CLIMBING, Sport.ORIENTEERING, Sport.CYCLING, Sport.RUNNING, Sport.TRIATHLON, Sport.SKATEBOARDING, Sport.ROLLER_SKATING -> "Açık hava"
        Sport.FITNESS, Sport.PILATES_YOGA, Sport.CROSSFIT, Sport.GYMNASTICS, Sport.WEIGHTLIFTING, Sport.DANCE, Sport.ATHLETICS -> "Antrenman"
        Sport.CUSTOM -> "Diğer"
        else -> if (isESport) "E-spor & zeka" else if (isTeamSport) "Takım" else "Hedef & beceri"
    }

// Search accepts Turkish names both with and without diacritics.
private fun String.searchKey() = lowercase().replace('ı', 'i').replace('ş', 's')
    .replace('ğ', 'g').replace('ü', 'u').replace('ö', 'o').replace('ç', 'c')

@Composable
fun SportPickerField(
    selectedSport: Sport?,
    onSelect: (Sport?) -> Unit,
    sports: List<Sport> = Sport.entries,
    allowAll: Boolean = true,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        onClick = { expanded = true },
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .shadow(
                elevation = 7.dp,
                shape = RoundedCornerShape(14.dp),
                ambientColor = Color(0x2C0B1220),
                spotColor = Color(0x3C0B1220)
            ),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFFD8E2EE)),
        color = Color.White,
        shadowElevation = 5.dp
    ) {
        Row(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color.White, Color(0xFFF8FAFC))))
            .padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF213E38),
                shadowElevation = 3.dp,
                modifier = Modifier.shadow(4.dp, RoundedCornerShape(10.dp), ambientColor = Color(0x330B1220), spotColor = Color(0x440B1220))
            ) {
                Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                    if (selectedSport != null) Text(selectedSport.emoji, fontSize = 16.sp)
                    else Icon(Icons.Rounded.Sports, null, Modifier.size(17.dp), tint = Color(0xFFE3CE9F))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(selectedSport?.label ?: "Tüm sporları keşfet", fontSize = 12.sp, lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold, color = Color(0xFF213E38), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${sports.size} branş · ara ve seç", fontSize = 10.sp, lineHeight = 14.sp,
                    color = MColors.Ink500, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Rounded.ChevronRight, null, Modifier.size(16.dp), tint = Color(0xFF527267))
        }
    }
    if (expanded) SportPickerSheet(
        selectedSport, sports, allowAll,
        onDismiss = { expanded = false },
        onSelect = { onSelect(it); expanded = false }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SportPickerSheet(
    selectedSport: Sport?,
    sports: List<Sport> = Sport.entries,
    allowAll: Boolean = true,
    onDismiss: () -> Unit,
    onSelect: (Sport?) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var family by remember { mutableStateOf<String?>(null) }
    val families = remember(sports) { sports.map { it.family }.distinct() }
    val results = remember(sports, query, family) {
        val key = query.trim().searchKey()
        sports.filter { (family == null || it.family == family) &&
            (it.label.searchKey().contains(key) || it.name.replace('_', ' ').searchKey().contains(key)) }
    }
    ModalBottomSheet(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 0.dp,
        scrimColor = androidx.compose.ui.graphics.Color(0xFF0B1220).copy(alpha = .48f),
dragHandle = { MPremiumSheetHandle() },
        
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MColors.Surface
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.88f).imePadding().padding(horizontal = 20.dp)) {
            MSheetHeading(
                title = "Sporunu bul",
                subtitle = "Her tempo, her seviye için bir branş.",
                icon = Icons.Rounded.Sports,
                onDismiss = onDismiss
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                    colors = com.matchora.app.ui.components.premiumTextFieldColors(),
                    textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium,

                value = query, onValueChange = { query = it }, singleLine = true,
                placeholder = { Text("Örn. yüzme, boks, raket…") },
                label = { Text("Spor ara") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                trailingIcon = if (query.isNotEmpty()) {{ IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, "Aramayı temizle") } }} else null,
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
                item { MChip("Tümü", family == null, { family = null }) }
                items(families) { item -> MChip(item, family == item, { family = item }) }
            }
            Text("${results.size} branş", style = MaterialTheme.typography.labelMedium, color = MColors.Ink500)
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (allowAll && query.isBlank() && family == null) item {
                    SportOptionRow("✦", "Tüm branşlar", "Filtreyi kaldır", selectedSport == null) { onSelect(null) }
                }
                items(results, key = { it.name }) { sport ->
                    SportOptionRow(sport.emoji, sport.label, sport.family, selectedSport == sport) { onSelect(sport) }
                }
                if (results.isEmpty()) item {
                    Column(Modifier.fillMaxWidth().padding(vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Bu aramada branş bulunamadı.", style = MaterialTheme.typography.titleMedium)
                        Text("Başka bir isim dene veya özel branşla devam et.", color = MColors.Ink500)
                        if (Sport.CUSTOM in sports) MButton("Özel / Diğer seç", { onSelect(Sport.CUSTOM) }, isPrimary = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun SportOptionRow(emoji: String, title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MColors.BrandTint else Color.White,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, if (selected) MColors.BrandDeep else Color(0xFFE4E9F2)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color(0x280B1220),
                spotColor = Color(0x380B1220)
            )
            .clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(14.dp).heightIn(min = 36.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MColors.Ink900)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
            }
            if (selected) Icon(Icons.Rounded.CheckCircle, "Seçili", tint = MColors.BrandDeep)
        }
    }
}
