package com.matchora.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.matchora.app.data.*
import com.matchora.app.state.Mission
import com.matchora.app.ui.theme.MColors

@Composable
fun RatingPolicyButton() {
    var show by remember { mutableStateOf(false) }
    TextButton(onClick = { show = true }) { Text("MMR nasıl değişir?") }
    if (show) RatingPolicySheet { show = false }
}

@Composable
fun RatingPolicySheet(onDismiss: () -> Unit) {
    val backend = LocalBackendRepository.current
    val config = if (backend != null) backend.configurations.collectAsState().value else null
    val policy = config?.o("ratingPolicy")
    val initial = policy?.i("initial", 1000) ?: 1000
    val hours = policy?.i("disputeHours", 24) ?: 24
    ActivitySheet("MMR ve gelişimin", "Puanının nereden geldiğini bil", onDismiss, Icons.Rounded.TrendingUp) {
        MFeatureHeader("Başlangıç $initial MMR", "Her branşın puanı ayrı hesaplanır", Icons.Rounded.EmojiEvents)
        Text("Rekabetçi maçta check-in yap. Skoru ve iki tarafın kadrosunu bildir. Rakip tarafın çoğunluğu sonucu onayladıktan ve $hours saatlik itiraz süresi bittikten sonra sunucu puanı hesaplar.")
        MSubCard {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Denk rakipte örnek", style = MaterialTheme.typography.titleSmall)
                Text("1000 ↔ 1000: galibiyet +12, mağlubiyet −12, beraberlik 0 MMR.")
                Text("Güçlü rakibi yenmek daha çok, zayıf rakibi yenmek daha az puan getirir. Takımlarda katılım kaydı olan oyuncuların ortalaması kullanılır.", style = MaterialTheme.typography.bodySmall)
            }
        }
        Text("Sosyal maç, itirazdaki sonuç veya eksik katılım kaydı MMR kazandırmaz. İki tarafta da katılım gerekir. Profilinden branşı kaldırıp eklemek puanını sıfırlamaz.", style = MaterialTheme.typography.bodySmall)
        Text("Görev, Token ve Plus MMR satın almaz. Görev ödülleri cüzdanına TKN olarak eklenir. Güvenilirlik puanın katılım kaydından ayrı hesaplanır.", style = MaterialTheme.typography.bodySmall, color = MColors.Ink600)
    }
}

@Composable
fun RatingStatusCard(eventId: String) {
    val backend = LocalBackendRepository.current
    val wire = if (backend != null) backend.snapshots.collectAsState().value else null
    val rating = wire?.a("results")?.firstOrNull { it.s("event_id") == eventId }?.o("rating")
    MSubCard {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (rating != null) {
                val reason = when (rating.s("reason")) {
                    "SOCIAL_MATCH" -> "Sosyal maç · MMR değişmez"
                    "CHECKIN_REQUIRED" -> "Katılım kaydın yok · MMR değişmez"
                    "OPPONENT_CHECKIN_REQUIRED" -> "Rakip katılım kaydı eksik · MMR değişmez"
                    "PROFILE_SPORT_REQUIRED" -> "Bu branş pasaportunda bulunmuyor"
                    "SIDES_REQUIRED" -> "Maç tarafları tamamlanmalı"
                    "DISPUTED" -> "İtiraz inceleniyor · puan beklemede"
                    "PENDING_CONFIRMATION" -> "Rakip onayı bekleniyor"
                    "VERIFIED" -> "Sonuç kesinleşti"
                    else -> "İtiraz süresinin bitmesi bekleniyor"
                }
                Text(reason, style = MaterialTheme.typography.titleSmall)
                if (rating.s("status") == "verified") {
                    val delta = rating.i("delta")
                    Text("Bu maç: ${if (delta > 0) "+" else ""}$delta MMR", color = MColors.BrandDeep)
                } else {
                    Text("Rakip onayı ${rating.i("confirmed")}/${rating.i("required", 1)} · ${if (rating.s("status") == "disputed") "operatör incelemesi" else "24 saatlik itiraz süresi"}", style = MaterialTheme.typography.bodySmall)
                }
            } else Text("MMR doğrulanmış rekabetçi sonuçtan oluşur.", style = MaterialTheme.typography.bodySmall)
            RatingPolicyButton()
        }
    }
}

@Composable
fun MissionsSheet(missions: List<Mission>, onDismiss: () -> Unit) {
    ActivitySheet("Haftalık görevlerin", "İlerlemeni gör, TKN ödüllerini takip et", onDismiss, Icons.Rounded.TaskAlt) {
        Text("Ödüller doğrulanmış maçlarla tamamlandığında cüzdanına bir kez yüklenir. MMR puanını değiştirmez.", style = MaterialTheme.typography.bodySmall)
        missions.forEach { mission ->
            MSubCard {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("${mission.emoji} ${mission.title}", style = MaterialTheme.typography.titleSmall)
                    Text(mission.description, style = MaterialTheme.typography.bodySmall)
                    LinearProgressIndicator(progress = { (mission.progress.toFloat() / mission.target.coerceAtLeast(1)).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                    Text("${mission.progress.coerceAtMost(mission.target)}/${mission.target} · +${mission.rewardPoints} TKN · ${if (mission.claimed) "Ödül yüklendi ✓" else "Devam ediyor"}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
