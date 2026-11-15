package com.matchora.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.matchora.app.data.*
import com.matchora.app.state.*
import com.matchora.app.ui.theme.MColors
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

val LocalBackendAction = staticCompositionLocalOf<((String, JsonObject) -> Unit)?> { null }

private data class PendingCommand(val operation: String, val payload: JsonObject, val key: String)

@Composable
fun BackendPlayerSafetyActions(player: Player) {
    val backend = LocalBackendRepository.current ?: return
    if (!backend.enabled || player.id == DemoData.ME) return
    val snapshot by backend.snapshots.collectAsState()
    val blocked = player.id in snapshot?.ss("blockedUserIds").orEmpty()
    val command = rememberBackendAction()
    var confirmBlock by remember { mutableStateOf(false) }
    var reporting by remember { mutableStateOf(false) }
    var reason by remember { mutableStateOf("") }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        TextButton(onClick = { confirmBlock = true }) { Text(if (blocked) "Engeli kaldır" else "Sporcuyu engelle", color = if (blocked) MColors.BrandDeep else MColors.Rose) }
        TextButton(onClick = { reporting = true }) { Text("Sporcuyu bildir", color = MColors.Rose) }
    }
    if (confirmBlock) AlertDialog(onDismissRequest = { confirmBlock = false }, title = { Text(if (blocked) "Engel kaldırılsın mı?" else "Sporcu engellensin mi?") },
        text = { Text(if (blocked) "Sporcu mesaj tercihlerin izin verdiğinde yeniden iletişim kurabilir." else "Karşılıklı mesajlaşma kapanır ve paylaşımlarınız birbirinizin vitrininden kaldırılır. Mevcut maç ve takım taahhütleri korunur.") },
        confirmButton = { TextButton(onClick = { command("user.block", apiPayload("userId" to backend.serverUserId(player.id), "blocked" to !blocked)); confirmBlock = false }) { Text(if (blocked) "Engeli kaldır" else "Engelle", color = if (blocked) MColors.BrandDeep else MColors.Rose) } },
        dismissButton = { TextButton(onClick = { confirmBlock = false }) { Text("Vazgeç") } })
    if (reporting) AlertDialog(onDismissRequest = { reporting = false }, title = { Text("Sporcuyu bildir") },
        text = { OutlinedTextField(reason, { reason = it.take(1000) }, label = { Text("Bildirim gerekçesi") }, minLines = 3) },
        confirmButton = { TextButton(enabled = reason.trim().length >= 3, onClick = { command("report.create", apiPayload("targetType" to "user", "targetId" to backend.serverUserId(player.id), "reason" to "Sporcu bildirimi", "details" to reason.trim())); reporting = false; reason = "" }) { Text("Bildir", color = MColors.Rose) } },
        dismissButton = { TextButton(onClick = { reporting = false }) { Text("Vazgeç") } })
}

@Composable
fun BackendChatMuteAction(thread: ChatThread) {
    val backend = LocalBackendRepository.current ?: return
    if (!backend.enabled || thread.locked) return
    val command = rememberBackendAction()
    TextButton(onClick = { command("chat.mute", apiPayload("threadId" to thread.id, "muted" to !thread.muted)) }) {
        Text(if (thread.muted) "Sohbet bildirimlerini aç" else "Sohbeti sessize al")
    }
}

@Composable
fun BackendIncomingTeamInvites(teams: List<TeamProfile>, onOpen: (TeamProfile) -> Unit) {
    val backend = LocalBackendRepository.current ?: return
    if (!backend.enabled) return
    val snapshot by backend.snapshots.collectAsState()
    val ids = snapshot?.a("incomingTeamInvites")?.map { it.s("team_id") }.orEmpty()
    teams.filter { it.id in ids }.forEach { team ->
        MCard(Modifier.fillMaxWidth()) { TextButton(onClick = { onOpen(team) }) { Text("${team.name} · Takım davetini incele") } }
    }
}

@Composable
internal fun rememberBackendAction(): (String, JsonObject) -> Unit {
    LocalBackendAction.current?.let { return it }
    val backend = LocalBackendRepository.current
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<PendingCommand?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val execute: () -> Unit = {
        val command = pending
        if (!busy && command != null && backend != null) scope.launch {
            busy = true
            try { backend.command(command.operation, command.payload, command.key); pending = null; error = null }
            catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (failure: Exception) { error = failure.message }
            finally { busy = false }
        }
    }
    error?.let { message -> AlertDialog(onDismissRequest = { error = null }, title = { Text("İşlem tamamlanamadı") }, text = { Text(message) },
        confirmButton = { TextButton(onClick = { error = null; execute() }) { Text("Tekrar dene") } },
        dismissButton = { TextButton(onClick = { error = null; pending = null }) { Text("Kapat") } }) }
    return { operation, payload ->
        if (!busy && backend?.enabled == true) { pending = PendingCommand(operation, payload, backend.requestKey("manage")); execute() }
    }
}

@Composable
fun BackendTeamActions(team: TeamProfile, players: List<Player>) {
    val backend = LocalBackendRepository.current ?: return
    if (!backend.enabled) return
    val snapshot by backend.snapshots.collectAsState()
    val command = rememberBackendAction()
    var confirmLeave by remember { mutableStateOf(false) }
    var confirmDeclineInvite by remember { mutableStateOf(false) }
    var transfer by remember { mutableStateOf(false) }
    var manageMembers by remember { mutableStateOf(false) }
    var removeMember by remember { mutableStateOf<Player?>(null) }
    var archive by remember { mutableStateOf(false) }
    val invited = snapshot?.a("incomingTeamInvites")?.any { it.s("team_id") == team.id } == true
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (invited) Row {
            TextButton(onClick = { command("team.invite.respond", apiPayload("teamId" to team.id, "accept" to true)) }) { Text("Takım davetini kabul et") }
            TextButton(onClick = { confirmDeclineInvite = true }) { Text("Reddet", color = MColors.Rose) }
        }
        if (team.isMine) {
            if (team.captainId == DemoData.ME) {
                TextButton(onClick = { manageMembers = true }) { Text("Takım kadrosunu yönet") }
                TextButton(onClick = { transfer = true }) { Text("Kaptanlığı devret") }
                TextButton(onClick = { archive = true }) { Text("Takımı kapat", color = MColors.Rose) }
            } else TextButton(onClick = { confirmLeave = true }) { Text("Takımdan ayrıl", color = MColors.Rose) }
        }
    }
    if (confirmDeclineInvite) AlertDialog(
        onDismissRequest = { confirmDeclineInvite = false },
        title = { Text("Takım daveti reddedilsin mi?") },
        text = { Text("${team.name} takımına ait daveti reddedeceksin.") },
        confirmButton = {
            TextButton(onClick = {
                confirmDeclineInvite = false
                command("team.invite.respond", apiPayload("teamId" to team.id, "accept" to false))
            }) { Text("Reddet", color = MColors.Rose) }
        },
        dismissButton = { TextButton(onClick = { confirmDeclineInvite = false }) { Text("Vazgeç") } }
    )
    if (manageMembers) AlertDialog(onDismissRequest = { manageMembers = false }, title = { Text("Takım kadrosu") }, text = {
        Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
            players.filter { it.id in team.memberIds && it.id != DemoData.ME }.forEach { player -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text(player.name, Modifier.weight(1f))
                TextButton(onClick = { removeMember = player }) { Text("Çıkar", color = MColors.Rose) }
            } }
            if (team.memberIds.size < 2) Text("Takımda başka sporcu yok.")
            val removed = snapshot?.a("removedTeamMembers")?.filter { it.s("team_id") == team.id }.orEmpty()
            if (removed.isNotEmpty()) Text("Çıkarılan sporcular", style = MaterialTheme.typography.titleSmall)
            removed.forEach { member -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text(snapshot?.a("players")?.firstOrNull { it.s("id") == member.s("user_id") }?.s("name") ?: "Sporcu", Modifier.weight(1f))
                TextButton(onClick = { command("team.invite", apiPayload("teamId" to team.id, "userId" to member.s("user_id"))) }) { Text("Yeniden davet et") }
            } }
        }
    }, confirmButton = { TextButton(onClick = { manageMembers = false }) { Text("Kapat") } })
    removeMember?.let { player -> AlertDialog(onDismissRequest = { removeMember = null }, title = { Text("Sporcu çıkarılsın mı?") }, text = { Text("${player.name} takım kadrosundan ve sohbetinden çıkarılacak. Resmi maç/turnuva taahhüdü varsa önce sonuçlandırılmalıdır.") },
        confirmButton = { TextButton(onClick = { command("team.member.remove", apiPayload("teamId" to team.id, "userId" to backend.serverUserId(player.id))); removeMember = null }) { Text("Takımdan çıkar", color = MColors.Rose) } },
        dismissButton = { TextButton(onClick = { removeMember = null }) { Text("Vazgeç") } }) }
    if (archive) AlertDialog(onDismissRequest = { archive = false }, title = { Text("Takım kapatılsın mı?") }, text = { Text("İlan, başvurular, davetler ve takım üyelikleri kapanacak. Bekleyen challenge taahhütleri iade edilir. Resmi kadro taahhüdü varsa işlem engellenir.") },
        confirmButton = { TextButton(onClick = { command("team.archive", apiPayload("teamId" to team.id)); archive = false; manageMembers = false }) { Text("Takımı kapat", color = MColors.Rose) } },
        dismissButton = { TextButton(onClick = { archive = false }) { Text("Vazgeç") } })
    if (confirmLeave) AlertDialog(onDismissRequest = { confirmLeave = false }, title = { Text("Takımdan ayrıl") }, text = { Text("Takım üyeliğin ve takım sohbetine erişimin kapanacak.") },
        confirmButton = { TextButton(onClick = { confirmLeave = false; command("team.leave", apiPayload("teamId" to team.id)) }) { Text("Ayrıl", color = MColors.Rose) } },
        dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Vazgeç") } })
    if (transfer) AlertDialog(onDismissRequest = { transfer = false }, title = { Text("Yeni kaptanı seç") },
        text = { Column { players.filter { it.id in team.memberIds && it.id != DemoData.ME }.forEach { player ->
            TextButton(onClick = { transfer = false; command("team.transfer", apiPayload("teamId" to team.id, "userId" to backend.serverUserId(player.id))) }) { Text(player.name) }
        }; if (team.memberIds.size < 2) Text("Kaptanlığı devretmek için başka bir takım üyesi gerekiyor.") } },
        confirmButton = { TextButton(onClick = { transfer = false }) { Text("Kapat") } })
}

@Composable
fun BackendCommunityActions(community: CommunityInfo) {
    val backend = LocalBackendRepository.current ?: return
    if (!backend.enabled) return
    val snapshot by backend.snapshots.collectAsState()
    val requests = snapshot?.a("communityRequests")?.filter { it.s("community_id") == community.id }.orEmpty()
    val players = snapshot?.a("players").orEmpty()
    val command = rememberBackendAction()
    val invited = snapshot?.a("incomingCommunityInvites")?.any { it.s("community_id") == community.id } == true
    var transfer by remember { mutableStateOf(false) }
    var invite by remember { mutableStateOf(false) }
    var edit by remember { mutableStateOf(false) }
    var remove by remember { mutableStateOf(false) }
    var removalTarget by remember { mutableStateOf<String?>(null) }
    var archiveCommunity by remember { mutableStateOf(false) }
    var confirmDeclineInvite by remember { mutableStateOf(false) }
    val owned = snapshot?.a("communities")?.any { it.s("id") == community.id && it.s("ownerId") == backend.userId } == true
    if (invited) Row {
        TextButton(onClick = { command("community.join", apiPayload("communityId" to community.id)) }) { Text("Topluluk davetini kabul et") }
        TextButton(onClick = { confirmDeclineInvite = true }) { Text("Reddet", color = MColors.Rose) }
    }
    if (confirmDeclineInvite) AlertDialog(
        onDismissRequest = { confirmDeclineInvite = false },
        title = { Text("Topluluk daveti reddedilsin mi?") },
        text = { Text("${community.name} topluluğuna ait daveti reddedeceksin.") },
        confirmButton = {
            TextButton(onClick = {
                confirmDeclineInvite = false
                command("community.invite.decline", apiPayload("communityId" to community.id))
            }) { Text("Reddet", color = MColors.Rose) }
        },
        dismissButton = { TextButton(onClick = { confirmDeclineInvite = false }) { Text("Vazgeç") } }
    )
    if (owned) TextButton(onClick = { archiveCommunity = true }) { Text("Topluluğu kapat", color = MColors.Rose) }
    if (archiveCommunity) AlertDialog(onDismissRequest = { archiveCommunity = false }, title = { Text("Topluluk kapatılsın mı?") }, text = { Text("Üyelikler ve başvurular kapanacak. Gelecekteki özel maçlar iptal edilip taahhütler iade edilir. Devam eden özel maç varsa önce sonuçlandırmalısın.") },
        confirmButton = { TextButton(onClick = { command("community.archive", apiPayload("communityId" to community.id)); archiveCommunity = false }) { Text("Topluluğu kapat", color = MColors.Rose) } },
        dismissButton = { TextButton(onClick = { archiveCommunity = false }) { Text("Vazgeç") } })
    if (owned) Row {
        TextButton(onClick = { edit = true }) { Text("Düzenle") }
        TextButton(onClick = { remove = true }) { Text("Üyeler") }
    }
    if (edit) {
        val original = remember { snapshot?.a("communities")?.firstOrNull { it.s("id") == community.id } }
        var name by remember { mutableStateOf(community.name) }
        var description by remember { mutableStateOf(community.description) }
        var access by remember { mutableStateOf(community.access) }
        var selectedSports by remember { mutableStateOf(community.sports.ifEmpty { listOf(com.matchora.app.domain.Sport.FOOTBALL) }) }
        AlertDialog(onDismissRequest = { edit = false }, title = { Text("Topluluğu düzenle") }, text = { Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(name, { name = it.take(80) }, label = { Text("Ad") })
            OutlinedTextField(description, { description = it.take(1000) }, label = { Text("Açıklama") })
            MFloatingDropdownBox("Katılım", access, CommunityAccess.entries, { it.label }, { access = it })
            Text("Branşlar", style = MaterialTheme.typography.labelMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                com.matchora.app.domain.Sport.entries.filter { it != com.matchora.app.domain.Sport.CUSTOM }.take(4).forEach { sp ->
                    val active = sp in selectedSports
                    FilterChip(active, {
                        selectedSports = if (active && selectedSports.size > 1) selectedSports - sp else (selectedSports + sp).distinct()
                    }, label = { Text(sp.emoji) })
                }
            }
            SportPickerField(selectedSports.firstOrNull(), { picked ->
                if (picked != null) {
                    selectedSports = if (picked in selectedSports && selectedSports.size > 1) selectedSports - picked else (listOf(picked) + selectedSports).distinct()
                }
            }, allowAll = false)
            Text("Seçili: ${selectedSports.joinToString(", ") { it.label }}", style = MaterialTheme.typography.bodySmall)
        } }, confirmButton = { TextButton(onClick = { command("community.update", apiPayload("communityId" to community.id, "expectedVersion" to (original?.i("version", 1) ?: 1), "name" to name, "description" to description, "access" to access, "sport" to selectedSports.first().name, "sports" to selectedSports.map { it.name })); edit = false }, enabled = name.trim().length >= 3) { Text("Kaydet") } }, dismissButton = { TextButton(onClick = { edit = false }) { Text("Vazgeç") } })
    }
    if (remove) {
        val members = snapshot?.a("ownedCommunityMembers")?.filter { it.s("community_id") == community.id && it.s("user_id") != backend.userId }.orEmpty()
        AlertDialog(onDismissRequest = { remove = false }, title = { Text("Topluluk üyeleri") }, text = { Column {
            if (members.isEmpty()) Text("Çıkarılabilecek başka üye yok.")
            members.forEach { member -> val id = member.s("user_id")
                TextButton(onClick = { removalTarget = id; remove = false }) { Text("${players.firstOrNull { it.s("id") == id }?.s("name") ?: id.take(8)} · Çıkar") }
            }
        } }, confirmButton = { TextButton(onClick = { remove = false }) { Text("Kapat") } })
    }
    removalTarget?.let { id -> AlertDialog(onDismissRequest = { removalTarget = null }, title = { Text("Üye çıkarılsın mı?") }, text = { Text("Üyelik ve gelecekteki özel maçlara katılımı kapanacak; ilgili taahhütler iade edilecek.") }, confirmButton = { TextButton(onClick = { command("community.member.remove", apiPayload("communityId" to community.id, "userId" to id)); removalTarget = null }) { Text("Üyeyi çıkar", color = MColors.Rose) } }, dismissButton = { TextButton(onClick = { removalTarget = null }) { Text("Vazgeç") } }) }
    if (owned) Row {
        TextButton(onClick = { transfer = true }) { Text("Yöneticiliği devret") }
        TextButton(onClick = { invite = true }) { Text("Sporcu davet et") }
    }
    if (transfer || invite) {
        val members = snapshot?.a("ownedCommunityMembers")?.filter { it.s("community_id") == community.id }?.map { it.s("user_id") }.orEmpty()
        val options = players.filter { it.s("id") != backend.userId && if (transfer) it.s("id") in members else it.s("id") !in members }
        AlertDialog(onDismissRequest = { transfer = false; invite = false }, title = { Text(if (transfer) "Yeni yöneticiyi seç" else "Sporcu davet et") }, text = { Column {
            if (options.isEmpty()) Text("Uygun sporcu bulunamadı.")
            options.take(30).forEach { player -> TextButton(onClick = {
                command(if (transfer) "community.transfer" else "community.invite", apiPayload("communityId" to community.id, "userId" to player.s("id")))
                transfer = false; invite = false
            }) { Text(player.s("name")) } }
        } }, confirmButton = { TextButton(onClick = { transfer = false; invite = false }) { Text("Kapat") } })
    }
    if (requests.isNotEmpty()) MCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
        Text("Topluluk üyelik talepleri", style = MaterialTheme.typography.titleSmall)
        requests.forEach { request -> Row {
            Text(players.firstOrNull { it.s("id") == request.s("user_id") }?.s("name") ?: "Sporcu", Modifier.weight(1f))
            TextButton(onClick = { command("community.review", apiPayload("communityId" to community.id, "userId" to request.s("user_id"), "approve" to true)) }) { Text("Onayla") }
            TextButton(onClick = { command("community.review", apiPayload("communityId" to community.id, "userId" to request.s("user_id"), "approve" to false)) }) { Text("Reddet", color = MColors.Rose) }
        } }
    } }
}

@Composable
fun BackendTournamentActions(tournament: TournamentInfo) {
    val backend = LocalBackendRepository.current
    if (tournament.creatorId != DemoData.ME || tournament.status in listOf("Tamamlandı", "İptal edildi")) return
    val snapshot by (backend?.snapshots?.collectAsState() ?: remember { mutableStateOf(null) })
    val liveVenueOptions = remember(snapshot, tournament.city) {
        snapshot?.let(::BackendSnapshot)?.venues.orEmpty()
            .filter { tournament.city.isBlank() || matchesTurkishPlace(it.city, tournament.city) }
            .map { "${it.name} · ${it.area}" }
            .distinct()
    }
    var show by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var editName by remember(tournament.id) { mutableStateOf(tournament.name) }
    var editDates by remember(tournament.id) { mutableStateOf(tournament.dates) }
    var editVenue by remember(tournament.id) { mutableStateOf(tournament.venue.orEmpty()) }
    var editRequirement by remember(tournament.id) { mutableStateOf(tournament.requirement) }
    var editFeeNote by remember(tournament.id) { mutableStateOf(tournament.feeNote) }
    var editPrizeToken by remember(tournament.id) { mutableStateOf(tournament.prizeToken.orEmpty()) }
    var editPrizeCash by remember(tournament.id) { mutableStateOf(tournament.prizeCash.orEmpty()) }
    var date by remember { mutableStateOf(kotlinx.datetime.LocalDate.fromEpochDays(todayDate().toEpochDays() + 1)) }
    var hour by remember { mutableIntStateOf(20) }
    var minute by remember { mutableIntStateOf(0) }
    var venue by remember(tournament.id) { mutableStateOf(tournament.venue.orEmpty()) }
    val command = rememberBackendAction()
    val completeLeague = tournament.isLeague && tournament.bracket.isNotEmpty()
    val latest = tournament.bracket.filter { it.round == tournament.bracket.maxOfOrNull { match -> match.round } }
    val finalReady = !tournament.isLeague && latest.size == 1 && latest.all { it.scoreA != null && it.scoreB != null }
    val ready = if (completeLeague) tournament.bracket.all { it.scoreA != null && it.scoreB != null }
        else if (tournament.bracket.isEmpty()) tournament.participants == tournament.capacity else latest.all { it.scoreA != null && it.scoreB != null }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = {
            if (backend != null && completeLeague) command("tournament.complete", apiPayload("tournamentId" to tournament.id))
            else if (backend != null && finalReady) command("tournament.schedule", apiPayload("tournamentId" to tournament.id, "startsAtEpochMillis" to (kotlinx.datetime.Clock.System.now().toEpochMilliseconds()+3600000), "venue" to latest.first().venue.ifBlank { tournament.venue ?: "Final sahası" }))
            else show = true
        }, enabled = (!completeLeague && !finalReady) || backend != null && ready) {
            Text(if (completeLeague) "Ligi sonuçlandır" else if (finalReady) "Şampiyonu kesinleştir" else if (tournament.bracket.isEmpty()) "Fikstürü planla" else "Sonraki turu planla")
        }
        if (backend?.enabled == true) {
            TextButton(onClick = {
                editName = tournament.name; editDates = tournament.dates; editVenue = tournament.venue.orEmpty()
                editRequirement = tournament.requirement; editFeeNote = tournament.feeNote
                editPrizeToken = tournament.prizeToken.orEmpty(); editPrizeCash = tournament.prizeCash.orEmpty()
                editing = true
            }) { Text("Turnuvayı düzenle") }
        }
    }
    if (!ready) Text(if (tournament.bracket.isEmpty()) "Fikstür için ${tournament.participants}/${tournament.capacity} takım kaydı tamamlandı." else "İlerlemek için önceki maçların sonuçları doğrulanmalı.", style = MaterialTheme.typography.bodySmall)
    if (show) AlertDialog(onDismissRequest = { show = false }, title = { Text("Turnuva planı") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EventDateTimeField(date, hour, minute) { d, h, m -> date = d; hour = h; minute = m }
            VenueChoiceField("Tesis / saha", venue, { venue = it.take(160) }, liveVenueOptions, tournament.city)
            Text("İlk turda kayıtların dolması, sonraki turda önceki sonuçların doğrulanması gerekiyor.", style = MaterialTheme.typography.bodySmall)
            if (backend == null) Text("Önizleme planı · fikstür yayınlamak için canlı organizatör hesabın gerekir.", style = MaterialTheme.typography.bodySmall)
        } }, confirmButton = { TextButton(onClick = { show = false; command("tournament.schedule", apiPayload("tournamentId" to tournament.id, "venue" to venue, "startsAtEpochMillis" to eventStartMillis(date, hour, minute))) }, enabled = backend?.authenticated == true && ready && venue.trim().length >= 2 && eventStartMillis(date,hour,minute)>kotlinx.datetime.Clock.System.now().toEpochMilliseconds()+60000) { Text("Planla") } },
        dismissButton = { TextButton(onClick = { show = false }) { Text("Vazgeç") } })
    if (editing) AlertDialog(onDismissRequest = { editing = false }, title = { Text("Turnuvayı düzenle") },
        text = { Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(editName, { editName = it.take(100) }, label = { Text("Turnuva adı") })
            OutlinedTextField(editDates, { editDates = it.take(80) }, label = { Text("Tarih aralığı") })
            VenueChoiceField("Tesis / saha", editVenue, { editVenue = it.take(160) }, liveVenueOptions, tournament.city)
            OutlinedTextField(editRequirement, { editRequirement = it.take(300) }, label = { Text("Katılım şartı & kurallar") })
            OutlinedTextField(editPrizeToken, { editPrizeToken = it.take(80) }, label = { Text("Token ödülü") })
            OutlinedTextField(editPrizeCash, { editPrizeCash = it.take(80) }, label = { Text("Ek ödül") })
            OutlinedTextField(editFeeNote, { editFeeNote = it.take(200) }, label = { Text("Ödül & katılım notu") })
        } }, confirmButton = { TextButton(enabled = editName.trim().length >= 3 && editDates.trim().length >= 2, onClick = {
            editing = false
            command("tournament.update", apiPayload(
                "tournamentId" to tournament.id,
                "expectedVersion" to tournament.version,
                "name" to editName.trim(),
                "dates" to editDates.trim(),
                "venue" to editVenue.trim().ifBlank { null },
                "requirement" to editRequirement.trim(),
                "prizeToken" to editPrizeToken.trim().ifBlank { null },
                "prizeCash" to editPrizeCash.trim().ifBlank { null },
                "feeNote" to editFeeNote.trim()
            ))
        }) { Text("Kaydet") } }, dismissButton = { TextButton(onClick = { editing = false }) { Text("Vazgeç") } })
}

@Composable
fun BackendTournamentRegistrationActions(tournament: TournamentInfo, registration: TournamentRegistration) {
    val backend = LocalBackendRepository.current ?: return
    if (!backend.enabled || tournament.creatorId != DemoData.ME || !tournament.status.contains("açık", true)) return
    var show by remember { mutableStateOf(false) }
    var reason by remember { mutableStateOf("") }
    val command = rememberBackendAction()
    val removed = registration.status == "removed"
    if (registration.status in listOf("joined", "removed")) TextButton(onClick = { show = true }) { Text(if (removed) "Yeniden kayda izin ver" else "Takım kaydını kaldır", color = if (removed) MColors.BrandDeep else MColors.Rose) }
    if (show) AlertDialog(onDismissRequest = { show = false }, title = { Text(if (removed) "Yeniden kayıt izni" else "Takım kaydı kaldırılsın mı?") }, text = { Column {
        Text(if (removed) "${registration.name} otomatik kaydolmaz. Kaptan yeniden kadro seçip başvurur." else "${registration.name} turnuvadan çıkarılır. Kulüp üyelikleri korunur; kaptan ve maç kadrosuna bildirim gider.")
        if (!removed) OutlinedTextField(reason, { reason = it.take(500) }, label = { Text("Gerekçe · en az 10 karakter") })
    } }, confirmButton = { TextButton(onClick = {
        command(if (removed) "tournament.registration.restore" else "tournament.registration.remove", if (removed) apiPayload("tournamentId" to tournament.id, "teamId" to registration.teamId) else apiPayload("tournamentId" to tournament.id, "teamId" to registration.teamId, "reason" to reason.trim()))
        show = false
    }, enabled = removed || reason.trim().length >= 10) { Text(if (removed) "İzin ver" else "Kaydı kaldır", color = if (removed) MColors.BrandDeep else MColors.Rose) } }, dismissButton = { TextButton(onClick = { show = false }) { Text("Vazgeç") } })
}
