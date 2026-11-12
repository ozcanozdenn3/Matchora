package com.matchora.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchora.app.data.*
import com.matchora.app.ui.theme.MColors
import kotlinx.coroutines.launch

/** Supply URLs here for preview builds, or publish them through the backend configuration. */
object AppLinkDefaults {
    val privacyPolicy: String? = "https://doc-hosting.flycricket.io/matchora/f7a31e53-cb1e-425b-8306-c4532fb98dcf/privacy"
    val termsOfUseIos: String = "https://www.apple.com/legal/internet-services/itunes/dev/stdeula/"
    val termsOfUseAndroid: String = "https://doc-hosting.flycricket.io/matchora-terms-of-use/d26b97eb-4228-41b8-80f2-b853c7ec2148/privacy"
    val termsOfUse: String get() = if (storePlatform == "android") termsOfUseAndroid else termsOfUseIos
    val communityGuidelines: String? = "http://192.168.0.5:8787/guidelines"
    val accountDeletion: String? = "http://192.168.0.5:8787/deletion"
    val supportEmail: String? = "ozden9865@gmail.com"
}
enum class AppLink(val key: String, val title: String, val missing: String) {
    PRIVACY("privacy", "Gizlilik Politikası", "Gizlilik politikası bağlantısı henüz eklenmedi. Paylaşıldığında bu alandan açılacak."),
    TERMS("terms", "Kullanım Koşulları", "Kullanım koşulları bağlantısı henüz eklenmedi. Paylaşıldığında bu alandan açılacak."),
    GUIDELINES("guidelines", "Topluluk kuralları", "Topluluk kuralları bağlantısı henüz eklenmedi."),
    DELETION("deletion", "Hesap ve veri silme bilgisi", "Hesap silme bilgi bağlantısı henüz eklenmedi. Hesap silme talebini hesap ayarlarından oluşturabilirsin."),
    SUPPORT("support", "Destek ile iletişime geç", "Destek e-posta adresi henüz eklenmedi. Uygulama içindeki destek formunu kullanabilirsin.")
}

@Composable
fun AppLinkButton(link: AppLink, modifier: Modifier = Modifier, compact: Boolean = false, prominent: Boolean = false) {
    val backend = LocalBackendRepository.current
    val config = if (backend != null) backend.configurations.collectAsState().value else null
    val configured = if (link == AppLink.TERMS) {
        val platformKey = if (storePlatform == "android") "terms_android" else "terms_ios"
        config?.o("documents")?.o(platformKey)?.takeIf { it.s("url").isNotBlank() } ?: config?.o("documents")?.o("terms")
    } else {
        config?.o("documents")?.o(link.key)
    }
    val fallback = when (link) { AppLink.PRIVACY -> AppLinkDefaults.privacyPolicy; AppLink.TERMS -> AppLinkDefaults.termsOfUse; AppLink.GUIDELINES -> AppLinkDefaults.communityGuidelines; AppLink.DELETION -> AppLinkDefaults.accountDeletion; AppLink.SUPPORT -> AppLinkDefaults.supportEmail }
    val target = if (link == AppLink.SUPPORT) configured?.s("email")?.ifBlank { null } ?: fallback else configured?.s("url")?.ifBlank { null } ?: fallback
    val uri = LocalUriHandler.current
    var message by remember { mutableStateOf<String?>(null) }
    val openLink: () -> Unit = {
        if (target == null) message = link.missing
        else if (link == AppLink.SUPPORT && Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+").matches(target)) runCatching { uri.openUri("mailto:$target") }.onFailure { message = "E-posta uygulaması açılamadı. Adres: $target" }
        else if (target.startsWith("https://") || target.startsWith("http://")) runCatching { uri.openUri(target) }.onFailure { message = "Bağlantı açılamadı. Daha sonra tekrar dene." }
        else message = "Bu bağlantı henüz kullanıma hazır değil."
    }
    if (prominent) OutlinedButton(onClick = openLink, modifier = modifier.height(42.dp),
        shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MColors.Line),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = MColors.Surface, contentColor = MColors.Ink800)) {
        Icon(if (link == AppLink.PRIVACY) Icons.Rounded.PrivacyTip else Icons.Rounded.Description, null, tint = MColors.BrandDeep, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(link.title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1)
    } else TextButton(onClick = openLink, modifier = modifier) {
        Text(link.title, color = MColors.Ink500, fontSize = if (compact) 11.sp else 12.sp)
        if (!compact) Icon(Icons.Rounded.ChevronRight, null, Modifier.size(16.dp), tint = MColors.Ink400)
    }
    message?.let { text -> AlertDialog(onDismissRequest = { message = null }, title = { Text(link.title) }, text = { Text(text) }, confirmButton = { TextButton(onClick = { message = null }) { Text("Tamam") } }) }
}

@Composable
fun AppLegalLinks(modifier: Modifier = Modifier, prominent: Boolean = false) {
    if (prominent) Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppLinkButton(AppLink.PRIVACY, modifier = Modifier.weight(1f), prominent = true)
        AppLinkButton(AppLink.TERMS, modifier = Modifier.weight(1f), prominent = true)
    } else Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        AppLinkButton(AppLink.PRIVACY, compact = true)
        AppLinkButton(AppLink.TERMS, compact = true)
    }
}

@Composable
fun LegalReviewButton() {
    val backend = LocalBackendRepository.current
    val config = if (backend != null) backend.configurations.collectAsState().value else null
    var visible by remember { mutableStateOf(false) }
    var checked by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    TextButton(onClick = { visible = true; checked = false; error = null }) { Text("Yasal belgeleri incele", color = MColors.Ink600) }
    if (visible) AlertDialog(onDismissRequest = { if (!busy) visible = false }, title = { Text("Yasal belgeler") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AppLegalLinks()
            if (config?.b("configured") != true) Text("Bağlantılar ve belge sürümleri paylaşıldığında buradan onaylayabilirsin.", color = MColors.Ink500)
            else Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked, { checked = it }); Text("Kullanım koşullarını ve gizlilik politikasını inceledim, kabul ediyorum.", style = MaterialTheme.typography.bodySmall) }
            error?.let { Text(it, color = MColors.Rose, style = MaterialTheme.typography.bodySmall) }
        } },
        confirmButton = { TextButton(onClick = { scope.launch { busy = true; try { backend?.acceptCurrentLegal(); visible = false } catch (e: Exception) { error = e.message } finally { busy = false } } }, enabled = config?.b("configured") == true && checked && !busy) { Text("Onayımı kaydet") } },
        dismissButton = { TextButton(onClick = { visible = false }, enabled = !busy) { Text("Kapat") } })
}
