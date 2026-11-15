package com.matchora.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.matchora.app.state.*
import com.matchora.app.ui.theme.MColors

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
    var selectedId by remember(tournament.id, withdraw) { mutableStateOf<String?>(options.firstOrNull()?.id) }
    var confirmWithdraw by remember(tournament.id, withdraw) { mutableStateOf(false) }
    val selected = options.firstOrNull { it.id == selectedId }
    var roster by remember(selectedId) { mutableStateOf(selected?.memberIds?.take(tournament.rosterSize ?: 50)?.toSet().orEmpty()) }
    ActivitySheet(if (withdraw) "Turnuva kaydını çek" else "Takım ve maç kadrosu", tournament.name, onDismiss, Icons.Rounded.Groups) {
        if (options.isEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (withdraw) "Kaptanı olduğun kayıtlı takım bulunamadı."
                    else "Bu turnuvaya katılabilmek için '${tournament.sport.label}' branşında kaptanı olduğun bir takım gerekiyor.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MColors.Ink600
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
        options.forEach { team ->
            val isSelected = selectedId == team.id
            MPremiumMenuRow(
                text = "${if (isSelected) "✓ " else ""}${team.badge} ${team.name}",
                subtitle = "${team.area} · ${team.memberIds.size} oyuncu · MMR ${team.rating}",
                icon = Icons.Rounded.Shield,
                onClick = { selectedId = team.id }
            )
        }
        selected?.let { team ->
            val duplicatePlayer = tournament.registrations.any { it.status == "joined" && it.teamId != team.id && it.rosterIds.any(roster::contains) }
            if (!withdraw) {
                Text(
                    text = tournament.rosterSize?.let { "Maç kadrosu: ${roster.size} / $it sporcu" } ?: "Maç kadrosu: ${roster.size} sporcu",
                    fontWeight = FontWeight.Bold,
                    color = MColors.Ink900
                )
                Text(
                    text = "Yalnız seçtiklerin turnuva maçlarına eklenir. Aynı turnuvada bir sporcu iki takımda oynayamaz.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MColors.Ink600
                )
                team.memberIds.forEach { id ->
                    val player = players.firstOrNull { it.id == id }
                    val displayName = player?.name ?: if (id == DemoData.ME) "Sen · kaptan" else "Takım sporcusu"
                    val isChecked = id in roster
                    Surface(
                        onClick = { roster = if (isChecked) roster - id else roster + id },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isChecked) MColors.BrandTint.copy(alpha = 0.45f) else Color.White,
                        border = BorderStroke(1.dp, if (isChecked) MColors.Brand.copy(alpha = 0.45f) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked -> roster = if (checked) roster + id else roster - id }
                            )
                            UserAvatar(
                                userId = id,
                                name = displayName,
                                modifier = Modifier.size(30.dp),
                                fallbackColor = player?.color ?: MColors.Brand
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = displayName,
                                fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium,
                                color = MColors.Ink900
                            )
                        }
                    }
                }
                if (duplicatePlayer) Text("Seçilen bir sporcu bu turnuvada başka bir takımın kadrosunda.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            if (withdraw) {
                MButton(
                    text = "Bu takımın kaydını çek",
                    onClick = { confirmWithdraw = true },
                    isPrimary = false,
                    isDestructive = true
                )
            } else {
                MButton(
                    text = "Kadroyla turnuvaya kaydol",
                    onClick = { onSubmit(team, roster.toList()) },
                    enabled = !duplicatePlayer && roster.isNotEmpty() && (tournament.rosterSize == null || roster.size == tournament.rosterSize)
                )
            }
            if (confirmWithdraw) {
                AlertDialog(
                    onDismissRequest = { confirmWithdraw = false },
                    title = { Text("Kaydı geri çek?") },
                    text = { Text("${team.name} takımının ${tournament.name} turnuvasındaki kaydı geri çekilecek. Kayıtlar açık olduğu sürece yeniden katılabilirsin.") },
                    confirmButton = {
                        TextButton(onClick = {
                            confirmWithdraw = false
                            onSubmit(team, roster.toList())
                        }) { Text("Kaydı geri çek", color = MColors.Rose) }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmWithdraw = false }) { Text("Vazgeç") }
                    }
                )
            }
        }
    }
}
