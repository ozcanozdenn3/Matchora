package com.matchora.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchora.app.state.LeaderboardRow

@Composable
fun AthleteAwardCard(athlete: LeaderboardRow, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(24.dp), color = Color(0xFF17212D), shadowElevation = 4.dp) {
        Row(Modifier.fillMaxWidth().drawBehind {
            val center = Offset(size.width * .92f, size.height * .2f)
            listOf(42.dp, 70.dp, 98.dp).forEach { drawCircle(Color(0xFFFFD48A).copy(alpha = .12f), it.toPx(), center, style = Stroke(1.dp.toPx())) }
        }.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(58.dp).shadow(8.dp, RoundedCornerShape(19.dp), ambientColor = Color(0x66000000), spotColor = Color(0x88000000)), contentAlignment = Alignment.Center) {
                UserAvatar(
                    userId = athlete.playerId,
                    name = athlete.name,
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(19.dp),
                    fallbackColor = Color(0xFFD4A64D),
                    fallbackTextColor = Color(0xFF45351D)
                )
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
