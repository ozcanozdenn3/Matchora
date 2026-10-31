package com.matchora.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchora.app.state.LeaderboardRow
import com.matchora.app.ui.theme.MColors

@Composable
fun AthleteAwardCard(athlete: LeaderboardRow, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(24.dp), color = Color(0xFF17212D), shadowElevation = 4.dp) {
        Row(Modifier.fillMaxWidth().drawBehind {
            val center = Offset(size.width * .92f, size.height * .2f)
            listOf(42.dp, 70.dp, 98.dp).forEach { drawCircle(Color(0xFFFFD48A).copy(alpha = .12f), it.toPx(), center, style = Stroke(1.dp.toPx())) }
        }.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(58.dp).shadow(8.dp, RoundedCornerShape(19.dp), ambientColor = Color(0x66000000), spotColor = Color(0x88000000)).background(Brush.linearGradient(listOf(Color(0xFFFFE5A7), Color(0xFFD4A64D))), RoundedCornerShape(19.dp)), contentAlignment = Alignment.Center) {
                Text(athlete.initials, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF45351D))
                Icon(Icons.Rounded.WorkspacePremium, null, Modifier.align(Alignment.BottomEnd).offset(x = 5.dp, y = 5.dp).size(21.dp), tint = Color(0xFFFFD48A))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("HAFTANIN SPORCUSU", color = Color(0xFFFFD48A), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(athlete.name, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("${athlete.rating} MMR   ·   ${athlete.verifiedMatches} maç", fontSize = 12.sp, color = Color(0xFFC7D0DB))
            }
            Icon(Icons.Rounded.ArrowForward, null, tint = Color(0xFFFFD48A), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun MatchScouterCard(onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(16.dp), color = Color(0xFF242239)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("TAKTİK LABORATUVARI", color = Color(0xFFC1B5FF), fontSize = 9.sp, letterSpacing = .8.sp, fontWeight = FontWeight.Bold)
                Text("Match Scouter", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Rakibini tanı. Oyununu planla.", color = Color(0xFFD1CDDF), fontSize = 11.5.sp, lineHeight = 16.sp)
                Text("Analizi aç →", color = Color(0xFFD4F3B3), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }
            Box(Modifier.size(54.dp).drawBehind {
                val center = Offset(size.width / 2, size.height / 2)
                listOf(.2f, .34f, .48f).forEach { drawCircle(Color(0xFFAB9DE2).copy(alpha = .45f), size.width * it, center, style = Stroke(1.dp.toPx())) }
                drawLine(Color(0xFFAB9DE2).copy(alpha = .3f), Offset(center.x, 0f), Offset(center.x, size.height), 1.dp.toPx())
                drawLine(Color(0xFFAB9DE2).copy(alpha = .3f), Offset(0f, center.y), Offset(size.width, center.y), 1.dp.toPx())
                drawCircle(Color(0xFFD4F3B3), 3.5.dp.toPx(), Offset(size.width * .7f, size.height * .3f))
            }, contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Insights, null, tint = Color(0xFFD4F3B3), modifier = Modifier.size(20.dp)) }
        }
    }
}

@Composable
fun CreateCupCard(onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(16.dp), color = Color(0xFFF9EEE0), border = BorderStroke(1.dp, Color(0xFFEBD6BB)), shadowElevation = 2.dp) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(44.dp).shadow(6.dp, RoundedCornerShape(14.dp), ambientColor = Color(0x360B1220), spotColor = Color(0x480B1220)).background(Brush.linearGradient(listOf(Color(0xFFFFD99B), Color(0xFFEFA96A))), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.EmojiEvents, null, Modifier.size(22.dp), tint = Color(0xFF78441E))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("KUPA MERKEZİ", color = Color(0xFF9B5A2B), fontSize = 9.sp, letterSpacing = .8.sp, fontWeight = FontWeight.Bold)
                Text("Kupanı oluştur", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MColors.Ink900)
                Text("Turnuvanı kur · formatı sen belirle", fontSize = 11.sp, color = Color(0xFF7B6554))
            }
            Icon(Icons.Rounded.AddCircle, null, tint = Color(0xFFB85D2D), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun CreateClubCard(onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(16.dp), color = Color(0xFFEAF2ED), border = BorderStroke(1.dp, Color(0xFFC7D9CC)), shadowElevation = 2.dp) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(44.dp).shadow(6.dp, RoundedCornerShape(14.dp), ambientColor = Color(0x360B1220), spotColor = Color(0x480B1220)).background(Color(0xFF243C32), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Shield, null, Modifier.size(24.dp), tint = Color(0xFFC3E0CB))
                Icon(Icons.Rounded.Add, null, Modifier.align(Alignment.BottomEnd).offset(x = 2.dp, y = 2.dp).size(15.dp).background(Color(0xFFEAF2ED), CircleShape).padding(1.dp), tint = Color(0xFF315B40))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("KULÜP KURULUMU", color = Color(0xFF42674D), fontSize = 9.sp, letterSpacing = .8.sp, fontWeight = FontWeight.Bold)
                Text("Kulübünü kur", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MColors.Ink900)
                Text("Armana karar ver · kadronu topla", fontSize = 11.sp, color = Color(0xFF5D7161))
            }
            Icon(Icons.Rounded.ArrowForward, null, tint = Color(0xFF42674D), modifier = Modifier.size(18.dp))
        }
    }
}
