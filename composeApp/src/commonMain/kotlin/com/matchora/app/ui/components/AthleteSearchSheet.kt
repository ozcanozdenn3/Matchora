package com.matchora.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.matchora.app.data.*
import com.matchora.app.domain.Sport
import com.matchora.app.state.Player
import com.matchora.app.state.label
import com.matchora.app.ui.theme.MColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AthleteSearchSheet(onDismiss: () -> Unit, onSelect: (Player) -> Unit, initialQuery: String = "") {
    val backend = LocalBackendRepository.current
    val owner = backend?.userId
    var query by remember { mutableStateOf(initialQuery) }
    var city by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var sport by remember { mutableStateOf<Sport?>(null) }
    var sportMenu by remember { mutableStateOf(false) }
    var rows by remember(owner, query, city, area, sport) { mutableStateOf(emptyList<JsonObject>()) }
    var cursor by remember(owner, query, city, area, sport) { mutableStateOf<String?>(null) }
    var loading by remember(owner, query, city, area, sport) { mutableStateOf(true) }
    var error by remember(owner, query, city, area, sport) { mutableStateOf<String?>(null) }
    var page by remember(owner, query, city, area, sport) { mutableIntStateOf(0) }
    var refresh by remember(owner, query, city, area, sport) { mutableIntStateOf(0) }
    LaunchedEffect(backend, owner, query, city, area, sport, page, refresh) {
        loading = true
        error = null
        delay(300)
        try {
            if (backend == null) {
                cursor = null
                error = null
            } else {
                val result = backend.searchPlayers(query, sport?.name, city, area, if (page == 0) null else cursor)
                rows = (if (page == 0) result.a("items") else rows + result.a("items")).distinctBy { it.s("id") }
                cursor = result.s("nextCursor").ifBlank { null }
            }
        } catch (cancel: CancellationException) { throw cancel }
        catch (failure: Exception) {
            if (failure is BackendFailure && failure.code == "INVALID_CURSOR") {
                rows = emptyList()
                cursor = null
                page = 0
            }
            error = failure.message ?: "Sporcular yüklenemedi."
        } finally { loading = false }
    }
    val players = remember(rows, backend?.userId, query, city, area, sport) {
        if (backend == null) {
            com.matchora.app.state.DemoData.players.filter { candidate ->
                (query.isBlank() || candidate.name.contains(query, ignoreCase = true) || candidate.area.contains(query, ignoreCase = true)) &&
                    (city.isBlank() || com.matchora.app.state.matchesTurkishPlace(candidate.city, city)) &&
                    (area.isBlank() || com.matchora.app.state.matchesTurkishPlace(candidate.area, area)) &&
                    (sport == null || sport in candidate.sports)
            }
        } else {
            BackendSnapshot(apiPayload("players" to rows, "userId" to backend.userId)).players
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss, containerColor = MColors.Canvas,
        modifier = Modifier.statusBarsPadding().padding(top = 12.dp),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { MPremiumSheetHandle() }
    ) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(.92f).padding(horizontal = 20.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MSheetHeading("Sporcu ara", "İsim, branş ve bölge", Icons.Rounded.PersonSearch, onDismiss)
            MPremiumTextField(query, { query = it.take(80) }, label = "Sporcu adı", leadingIcon = Icons.Rounded.Search)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MPremiumTextField(city, { city = it.take(60) }, label = "İl · isteğe bağlı", modifier = Modifier.weight(1f))
                MPremiumTextField(area, { area = it.take(60) }, label = "İlçe · isteğe bağlı", modifier = Modifier.weight(1f))
            }
            Box {
                OutlinedButton(onClick = { sportMenu = true }) {
                    Icon(Icons.Rounded.Sports, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(sport?.label ?: "Tüm branşlar")
                }
                DropdownMenu(
                    expanded = sportMenu, onDismissRequest = { sportMenu = false },
                    modifier = Modifier.heightIn(max = 320.dp)
                ) {
                    DropdownMenuItem(text = { Text("Tüm branşlar") }, onClick = { sport = null; sportMenu = false })
                    Sport.entries.forEach { option ->
                        DropdownMenuItem(text = { Text(option.label) }, onClick = { sport = option; sportMenu = false })
                    }
                }
            }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = MColors.Brand)
            error?.let { message ->
                Text(message, color = MColors.Rose, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { refresh++ }) { Text("Tekrar dene") }
            }
            LazyColumn(
                Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                if (!loading && error == null && players.isEmpty()) {
                    item { Text("Bu aramada sporcu bulunamadı.", color = MColors.Ink500) }
                }
                items(players, key = { it.id }) { player ->
                    MCard(Modifier.fillMaxWidth(), onClick = { onSelect(player) }) {
                        Row(
                            Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            UserAvatar(player.id, player.name, Modifier.size(42.dp))
                            Column(Modifier.weight(1f)) {
                                Text(player.name, style = MaterialTheme.typography.titleSmall)
                                Text(player.area, style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
                            }
                            Icon(Icons.Rounded.ChevronRight, null, tint = MColors.BrandDeep)
                        }
                    }
                }
                if (cursor != null) item {
                    TextButton(
                        onClick = { loading = true; page++ }, enabled = !loading,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Daha fazla sporcu yükle") }
                }
            }
        }
    }
}
