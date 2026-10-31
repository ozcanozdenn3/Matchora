package com.matchora.app.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchora.app.state.AppNotification
import com.matchora.app.state.NotifPriority
import com.matchora.app.ui.theme.MColors

// Filtering and notification actions remain shared with the app's notification state.
enum class NotifFilterCategory(val label: String, val icon: ImageVector) {
    ALL("Tümü", Icons.Rounded.NotificationsNone),
    MATCHES("Maçlar", Icons.Rounded.SportsSoccer),
    SQUADS("Kadro & İstekler", Icons.Rounded.Groups),
    WALLET("Cüzdan", Icons.Rounded.AccountBalanceWallet)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSheet(
    notifications: List<AppNotification>,
    onDismiss: () -> Unit,
    onNotificationClick: (AppNotification) -> Unit,
    onMarkAllRead: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(NotifFilterCategory.ALL) }
    var showPreferences by remember { mutableStateOf(false) }
    var quietHoursActive by remember { mutableStateOf(true) }
    var emergencyCallsActive by remember { mutableStateOf(true) }
    val unreadCount = notifications.count { !it.read }
    val filteredList = remember(notifications, selectedCategory) {
        when (selectedCategory) {
            NotifFilterCategory.ALL -> notifications
            NotifFilterCategory.MATCHES -> notifications.filter { it.priority == NotifPriority.P1 || it.priority == NotifPriority.P2 }
            NotifFilterCategory.SQUADS -> notifications.filter {
                it.priority == NotifPriority.P3 || it.title.contains("Kadro", true) || it.title.contains("Başvuru", true)
            }
            NotifFilterCategory.WALLET -> notifications.filter {
                it.priority == NotifPriority.P0 || it.title.contains("Token", true) || it.title.contains("Tesis", true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.statusBarsPadding().padding(top = 12.dp),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = Color(0xFFF4F5F7),
        tonalElevation = 0.dp,
        scrimColor = MColors.Ink900.copy(alpha = .48f),
        dragHandle = { MPremiumSheetHandle() }
    ) {
        Column(Modifier.fillMaxWidth().heightIn(max = 720.dp)) {
            Surface(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                shape = RoundedCornerShape(21.dp),
                color = MColors.Ink900,
                border = BorderStroke(1.dp, Color(0xFF3C4857))
            ) {
                Column(Modifier.background(Brush.linearGradient(listOf(Color(0xFF2A3B46), MColors.Ink900)))
                    .drawBehind {
                        val center = Offset(size.width * .93f, size.height * .05f)
                        listOf(44.dp, 66.dp, 88.dp).forEach {
                            drawCircle(Color(0xFFFFC799).copy(alpha = .10f), it.toPx(), center, style = Stroke(.8.dp.toPx()))
                        }
                    }.padding(15.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(shape = RoundedCornerShape(13.dp), color = Color(0xFF46504F)) {
                            Icon(Icons.Rounded.NotificationsActive, null, Modifier.padding(10.dp).size(21.dp), tint = Color(0xFFFFC799))
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Bildirimler", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Sahanın gündemi, tek yerde.", fontSize = 12.sp, color = Color(0xFFBAC8D2))
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Rounded.Close, "Kapat", tint = Color(0xFFDEE5EB), modifier = Modifier.size(20.dp))
                        }
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = .12f))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF3F423E)) {
                            Row(Modifier.padding(horizontal = 9.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(Modifier.size(5.dp).background(if (unreadCount > 0) Color(0xFFFFB87F) else Color(0xFF9EDDC8), CircleShape))
                                Text(if (unreadCount > 0) "$unreadCount okunmamış" else "Hepsi güncel", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFFFD6B4))
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = onMarkAllRead, enabled = unreadCount > 0,
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFE8D4BE), disabledContentColor = Color(0xFF8D99A4)),
                            contentPadding = PaddingValues(horizontal = 8.dp)) {
                            Icon(Icons.Rounded.DoneAll, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Tümünü oku", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        IconToggleButton(checked = showPreferences, onCheckedChange = { showPreferences = it }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Rounded.Tune, "Bildirim tercihleri", tint = if (showPreferences) Color(0xFFFFB87F) else Color(0xFFCBD5DF), modifier = Modifier.size(19.dp))
                        }
                    }
                }
            }

            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(NotifFilterCategory.entries.toList(), key = { it.name }) { category ->
                    val selected = category == selectedCategory
                    Surface(onClick = { selectedCategory = category }, shape = RoundedCornerShape(12.dp),
                        color = if (selected) MColors.Ink900 else Color.White,
                        border = BorderStroke(1.dp, if (selected) Color(0xFF344553) else MColors.Line)) {
                        Row(Modifier.padding(horizontal = 11.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(category.icon, null, Modifier.size(15.dp), tint = if (selected) Color(0xFFFFC799) else MColors.Ink500)
                            Text(category.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (selected) Color.White else MColors.Ink600)
                        }
                    }
                }
            }

            LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                item(key = "preferences") {
                    AnimatedVisibility(visible = showPreferences) {
                        Surface(shape = RoundedCornerShape(18.dp), color = Color.White, border = BorderStroke(1.dp, MColors.Line)) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("SANA NASIL ULAŞALIM?", fontSize = 10.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold, color = MColors.Ink500)
                                NotificationPreference(Icons.Rounded.NightsStay, "Sessiz saatler", "23:00 – 08:00 · Acil olmayan çağrılar bekler", quietHoursActive, { quietHoursActive = it })
                                HorizontalDivider(color = MColors.LineSoft)
                                NotificationPreference(Icons.Rounded.Bolt, "Acil maç çağrıları", "Yakınındaki eksik kadrolardan haberdar ol", emergencyCallsActive, { emergencyCallsActive = it })
                            }
                        }
                    }
                }
                item(key = "section") {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (selectedCategory == NotifFilterCategory.ALL) "SON HAREKETLER" else selectedCategory.label.uppercase(), Modifier.weight(1f), fontSize = 10.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold, color = MColors.Ink500)
                        Text("${filteredList.size} bildirim", fontSize = 11.sp, color = MColors.Ink400)
                    }
                }
                if (filteredList.isEmpty()) {
                    item(key = "empty") {
                        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, border = BorderStroke(1.dp, MColors.Line)) {
                            Column(Modifier.fillMaxWidth().padding(vertical = 30.dp, horizontal = 18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Surface(shape = RoundedCornerShape(18.dp), color = MColors.BrandTint) {
                                    Icon(Icons.Rounded.NotificationsNone, null, Modifier.padding(14.dp).size(25.dp), tint = MColors.BrandDeep)
                                }
                                Text("Şimdilik her şey sakin", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MColors.Ink900)
                                Text("Bu kategorideki yeni bildirimler burada görünecek.", fontSize = 12.sp, color = MColors.Ink500, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                    }
                }
                items(filteredList, key = { it.id }) { notification ->
                    NotificationInboxCard(notification, { onNotificationClick(notification) })
                }
            }
        }
    }
}

@Composable
private fun NotificationPreference(icon: ImageVector, title: String, detail: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(shape = RoundedCornerShape(10.dp), color = MColors.SurfaceSunken) {
            Icon(icon, null, Modifier.padding(8.dp).size(17.dp), tint = MColors.Ink600)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MColors.Ink900)
            Text(detail, fontSize = 11.sp, lineHeight = 15.sp, color = MColors.Ink500)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MColors.BrandDeep, uncheckedTrackColor = MColors.SurfaceSunken, uncheckedBorderColor = MColors.Ink200))
    }
}

@Composable
private fun NotificationInboxCard(notification: AppNotification, onClick: () -> Unit) {
    val unread = !notification.read
    val accent = notification.priority.color
    val icon = when (notification.priority) {
        NotifPriority.P0 -> Icons.Rounded.Shield
        NotifPriority.P1 -> Icons.Rounded.SportsSoccer
        NotifPriority.P2 -> Icons.Rounded.EmojiEvents
        NotifPriority.P3 -> Icons.Rounded.Groups
        NotifPriority.P4 -> Icons.Rounded.AutoAwesome
    }
    Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(18.dp),
        color = Color.White, border = BorderStroke(1.dp, if (unread) accent.copy(alpha = .24f) else MColors.Line), shadowElevation = if (unread) 2.dp else 0.dp) {
        Row(Modifier.fillMaxWidth().drawBehind {
            if (unread) drawLine(accent, Offset(1.5.dp.toPx(), 18.dp.toPx()), Offset(1.5.dp.toPx(), size.height - 18.dp.toPx()), 3.dp.toPx())
        }.padding(13.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(shape = RoundedCornerShape(13.dp), color = if (unread) notification.priority.tint else MColors.SurfaceMuted,
                border = BorderStroke(.5.dp, accent.copy(alpha = .14f))) {
                Icon(icon, null, Modifier.padding(10.dp).size(19.dp), tint = if (unread) accent else MColors.Ink500)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(notification.title, Modifier.weight(1f), fontSize = 13.sp, lineHeight = 18.sp, fontWeight = if (unread) FontWeight.Bold else FontWeight.SemiBold, color = MColors.Ink900)
                    if (unread) Box(Modifier.size(6.dp).background(accent, CircleShape))
                }
                Text(notification.body, fontSize = 12.sp, lineHeight = 17.sp, color = MColors.Ink600)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(notification.timeAgo, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MColors.Ink400)
                    Spacer(Modifier.weight(1f))
                    if (notification.eventId != null) {
                        Surface(shape = RoundedCornerShape(7.dp), color = notification.priority.tint) {
                            Row(Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text("Etkinliği aç", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accent)
                                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(12.dp), tint = accent)
                            }
                        }
                    }
                }
            }
        }
    }
}
