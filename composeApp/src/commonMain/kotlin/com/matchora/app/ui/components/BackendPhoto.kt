package com.matchora.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchora.app.data.*
import com.matchora.app.state.DemoData
import com.matchora.app.state.Player
import com.matchora.app.ui.theme.MColors

val LocalBackendRepository = staticCompositionLocalOf<BackendRepository?> { null }

@Composable
fun BackendPhoto(
    uploadId: String,
    description: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val backend = LocalBackendRepository.current
    var image by remember(uploadId) { mutableStateOf<ImageBitmap?>(null) }
    var failed by remember(uploadId) { mutableStateOf(false) }
    var attempt by remember(uploadId) { mutableIntStateOf(0) }
    LaunchedEffect(uploadId, attempt) {
        failed = false
        try { image = backend?.photo(uploadId); failed = image == null }
        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { failed = true }
    }
    if (image != null) Image(requireNotNull(image), description, modifier, contentScale = contentScale)
    else Box(modifier.background(MColors.SurfaceMuted), contentAlignment = Alignment.Center) {
        if (failed) TextButton(onClick = { attempt++ }) { Icon(Icons.Rounded.BrokenImage, description, Modifier.size(18.dp)); Text("Tekrar dene") }
        else CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
    }
}

@Composable
fun userAvatarUploadId(userId: String): String? {
    val backend = LocalBackendRepository.current ?: return null
    val wire by backend.snapshots.collectAsState()
    val serverId = backend.serverUserId(userId)
    val sessionUser = backend.userId

    // 1) Oturum sahibi ise ve profilde avatarUploadId tanımlıysa
    if (serverId == sessionUser || userId == DemoData.ME || (sessionUser != null && userId == sessionUser) || userId == "me") {
        val myProfile = wire?.o("profile")
        val myAvatar = myProfile?.s("avatarUploadId")?.ifBlank { null }
        if (myAvatar != null) return myAvatar
    }

    // 2) Diğer oyuncular listesinde ara
    val fromPlayers = wire?.a("players")?.firstOrNull { it.s("id") == serverId || it.s("id") == userId }?.s("avatarUploadId")?.ifBlank { null }
    if (fromPlayers != null) return fromPlayers

    // 3) Sıralamalarda (rankings) ara
    val fromRankings = wire?.a("rankings")?.firstOrNull { it.s("id") == serverId || it.s("id") == userId || it.s("userId") == serverId || it.s("userId") == userId }?.s("avatarUploadId")?.ifBlank { null }
    if (fromRankings != null) return fromRankings

    return null
}

@Composable
fun playerAvatarUploadId(player: Player): String? {
    return userAvatarUploadId(player.id)
}

@Composable
fun UserAvatar(
    userId: String,
    name: String,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    fallbackColor: Color = MColors.Brand,
    fallbackTextColor: Color = Color.White,
    localAvatar: ImageBitmap? = null
) {
    if (localAvatar != null) {
        Image(
            bitmap = localAvatar,
            contentDescription = name,
            modifier = modifier.clip(shape),
            contentScale = ContentScale.Crop
        )
        return
    }

    val uploadId = userAvatarUploadId(userId)
    if (uploadId != null) {
        BackendPhoto(
            uploadId = uploadId,
            description = name,
            modifier = modifier.clip(shape)
        )
    } else {
        val initials = name.split(" ")
            .filter { it.isNotBlank() }
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .take(2)
            .joinToString("")
            .ifBlank { name.take(2).uppercase().ifBlank { "?" } }
        Box(
            modifier = modifier
                .clip(shape)
                .background(fallbackColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = fallbackTextColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

