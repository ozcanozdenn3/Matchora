package com.matchora.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.matchora.app.state.*

@Composable
fun TournamentRegistrationSheet(
    tournament: TournamentInfo,
    teams: List<TeamProfile>,
    players: List<Player>,
    withdraw: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (TeamProfile, List<String>) -> Unit,
    onCreateNewTeam: (() -> Unit)? = null
) {
    val registeredIds = tournament.registrations.filter { if (withdraw) it.status == "joined" else it.status in listOf("joined", "removed") }.map { it.teamId }.toSet()
    val options = teams.filter { it.captainId == DemoData.ME && it.sport == tournament.sport && if (withdraw) it.id in registeredIds else it.id !in registeredIds }
    var selectedId by remember(tournament.id, withdraw) { mutableStateOf<String?>(null) }
    val selected = options.firstOrNull { it.id == selectedId }
    var roster by remember(selectedId) { mutableStateOf(selected?.memberIds?.take(tournament.rosterSize ?: 50)?.toSet().orEmpty()) }
    ActivitySheet(if (withdraw) "Turnuva kaydını çek" else "Takım ve maç kadrosu", tournament.name, onDismiss, Icons.Rounded.Groups) {
        if (options.isEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (withdraw) "Kaptanı olduğun kayıtlı takım bulunamadı."
                    else "Bu turnuvaya katılabilmek için '${tournament.sport.label}' branşında kaptanı olduğun bir takım gerekiyor.",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (!withdraw && onCreateNewTeam != null) {
                    MButton(
                        text = "Yeni Takım Kur 🛡️",
                        onClick = {
                            onDismiss()
                            onCreateNewTeam()
                        },
                        icon = Icons.Rounded.Add
                    )
                }
            }
        }
        options.forEach { team -> OutlinedButton(onClick = { selectedId = team.id }, modifier = Modifier.fillMaxWidth()) { Text("${if (selectedId == team.id) "✓ " else ""}${team.badge} ${team.name}") } }
        selected?.let { team ->
            val duplicatePlayer = tournament.registrations.any { it.status == "joined" && it.teamId != team.id && it.rosterIds.any(roster::contains) }
            if (!withdraw) {
                Text(tournament.rosterSize?.let { "Maç kadrosu: ${roster.size} / $it sporcu" } ?: "Maç kadrosu: ${roster.size} sporcu")
                Text("Yalnız seçtiklerin turnuva maçlarına eklenir. Aynı turnuvada bir sporcu iki takımda oynayamaz.", style = MaterialTheme.typography.bodySmall)
                team.memberIds.forEach { id -> 
                    val player = players.firstOrNull { it.id == id }
                    val displayName = player?.name ?: if (id == DemoData.ME) "Sen · kaptan" else "Takım sporcusu"
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                        Checkbox(id in roster, { checked -> roster = if (checked) roster + id else roster - id })
                        UserAvatar(
                            userId = id,
                            name = displayName,
                            modifier = Modifier.size(28.dp),
                            fallbackColor = player?.color ?: com.matchora.app.ui.theme.MColors.Brand
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(displayName)
                    }
                }
                if (duplicatePlayer) Text("Seçilen bir sporcu bu turnuvada başka bir takımın kadrosunda.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            MButton(if (withdraw) "Bu takımın kaydını çek" else "Kadroyla turnuvaya kaydol", { onSubmit(team, roster.toList()) },
                enabled = withdraw || !duplicatePlayer && roster.isNotEmpty() && (tournament.rosterSize == null || roster.size == tournament.rosterSize))
        }
    }
}
