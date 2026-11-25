package com.matchora.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.matchora.app.data.*
import com.matchora.app.ui.components.*
import com.matchora.app.ui.theme.MColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.JsonObject

private data class AdminActionSpec(
    val title: String,
    val operation: String,
    val payload: JsonObject,
    val field: String? = "reason",
    val label: String = "Gerekçe",
    val minimum: Int = 10,
    val presetText: String = "",
    val message: String? = null,
    val destructive: Boolean = false
)

@Composable
fun AdminConsoleScreen(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    unreadNotificationsCount: Int,
    refreshSignal: Int,
    onOpenNotifications: () -> Unit,
    onOpenVenueOperator: () -> Unit,
    onLogout: () -> Unit
) {
    val backend = LocalBackendRepository.current ?: return
    val wire by backend.snapshots.collectAsState()
    val toast = LocalToastNotifier.current
    val scope = rememberCoroutineScope()

    var data by remember { mutableStateOf<JsonObject?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var localRefresh by remember { mutableIntStateOf(0) }
    var activeAction by remember { mutableStateOf<AdminActionSpec?>(null) }
    var previewUploadId by remember { mutableStateOf<String?>(null) }
    var confirmLogout by remember { mutableStateOf(false) }

    LaunchedEffect(backend, backend.userId, refreshSignal, localRefresh, searchQuery) {
        if (searchQuery.isNotBlank()) kotlinx.coroutines.delay(300)
        loading = true
        error = null
        try {
            data = backend.adminInbox(searchQuery)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (failure: Exception) {
            error = (failure as? BackendFailure)?.message ?: "Yönetim verileri yüklenemedi."
        } finally {
            loading = false
        }
    }

    val photos = remember(data) { data?.a("photos").orEmpty() }
    val support = remember(data) { data?.a("support").orEmpty() }
    val reports = remember(data) { data?.a("reports").orEmpty() }
    val memories = remember(data) { data?.a("memories").orEmpty() }
    val venueApps = remember(data) { data?.a("venueApplications").orEmpty() }
    val reviews = remember(data) { data?.a("reviews").orEmpty() }
    val results = remember(data) { data?.a("results").orEmpty() }
    val userDirectory = remember(data) { data?.a("userDirectory").orEmpty() }
    val users = remember(data, userDirectory) { userDirectory.ifEmpty { data?.a("users").orEmpty() } }
    val featureFlags = remember(data) { data?.a("featureFlags").orEmpty() }
    val announcements = remember(data) { data?.a("announcements").orEmpty() }
    val outboxFailures = remember(data) { data?.a("outboxFailures").orEmpty() }
    val identityDeletions = remember(data) { data?.a("identityDeletions").orEmpty() }
    val deletionRequests = remember(data) { data?.a("deletionRequests").orEmpty() }
    val recentAudit = remember(data) { data?.a("recentAudit").orEmpty() }
    val legalDocs = remember(data) { data?.o("legalDocuments") ?: JsonObject(emptyMap()) }

    val badgeTab0 = photos.size + support.size
    val badgeTab1 = reports.size + memories.size
    val badgeTab2 = venueApps.size + reviews.size + results.size
    val badgeTab3 = outboxFailures.size + identityDeletions.size + deletionRequests.size
    val totalPending = badgeTab0 + badgeTab1 + badgeTab2 + badgeTab3

    Scaffold(
        containerColor = MColors.Canvas,
        topBar = {
            AdminTopBar(
                totalPending = totalPending,
                unreadNotifications = unreadNotificationsCount,
                loading = loading,
                onRefresh = { localRefresh++ },
                onNotifications = onOpenNotifications,
                onVenueOperator = onOpenVenueOperator,
                onLogout = { confirmLogout = true }
            )
        },
        bottomBar = {
            AdminBottomBar(
                selectedTab = selectedTab,
                onSelectTab = onSelectTab,
                badges = listOf(badgeTab0, badgeTab1, badgeTab2, badgeTab3)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (loading) {
                item {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MColors.Brand)
                }
            }
            error?.let { err ->
                item {
                    MCard(borderColor = MColors.Rose.copy(alpha = 0.4f), backgroundColor = Color(0xFFFFF1F2)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(err, color = MColors.Rose, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            MPillActionButton("Tekrar Dene", { localRefresh++ }, icon = Icons.Rounded.Refresh, tone = MPillTone.DANGER, compact = true)
                        }
                    }
                }
            }

            // Canlı Özet Kuyruk Çubuğu
            item {
                AdminQueueStrip(
                    photos = photos.size,
                    support = support.size,
                    reports = reports.size,
                    memories = memories.size,
                    venueApps = venueApps.size,
                    reviews = reviews.size,
                    results = results.size,
                    failures = outboxFailures.size + identityDeletions.size,
                    onJump = onSelectTab
                )
            }

            when (selectedTab) {
                // 0. TALEPLER: Fotoğraf Doğrulama + Destek
                0 -> {
                    item { AdminSectionHeader("Fotoğraf Doğrulamaları", photos.size, Icons.Rounded.VerifiedUser) }
                    if (photos.isEmpty() && !loading) {
                        item { AdminEmptyState("İnceleme bekleyen profil fotoğrafı yok.") }
                    }
                    items(photos, key = { "photo_${it.s("id")}" }) { req ->
                        AdminPhotoVerificationCard(
                            req = req,
                            onZoomPhoto = { previewUploadId = it },
                            onApprove = {
                                activeAction = AdminActionSpec(
                                    title = "Fotoğraf Doğrulamasını Onayla",
                                    operation = "admin.verification.photo",
                                    payload = apiPayload("userId" to req.s("user_id"), "uploadId" to req.s("uploadId"), "requestId" to req.s("id"), "approved" to true),
                                    label = "Karar gerekçesi",
                                    minimum = 3,
                                    presetText = "Profil fotoğrafın net ve topluluk kurallarına uygun."
                                )
                            },
                            onReject = {
                                activeAction = AdminActionSpec(
                                    title = "Fotoğraf Doğrulamasını Reddet",
                                    operation = "admin.verification.photo",
                                    payload = apiPayload("userId" to req.s("user_id"), "uploadId" to req.s("uploadId"), "requestId" to req.s("id"), "approved" to false),
                                    label = "Red gerekçesi",
                                    minimum = 3,
                                    presetText = "Yüzünün net göründüğü aydınlık bir profil fotoğrafı yüklemelisin.",
                                    destructive = true
                                )
                            }
                        )
                    }

                    item { AdminSectionHeader("Destek Talepleri", support.size, Icons.Rounded.SupportAgent) }
                    if (support.isEmpty() && !loading) {
                        item { AdminEmptyState("Açık destek talebi yok.") }
                    }
                    items(support, key = { "support_${it.s("id")}" }) { req ->
                        AdminSupportCard(
                            req = req,
                            onResolve = {
                                activeAction = AdminActionSpec(
                                    title = "Destek Talebini Yanıtla",
                                    operation = "admin.support.resolve",
                                    payload = apiPayload("requestId" to req.s("id"), "status" to "resolved"),
                                    field = "response",
                                    label = "Kullanıcıya yanıt",
                                    minimum = 3,
                                    presetText = "Talebin incelendi ve çözüme kavuşturuldu."
                                )
                            },
                            onReject = {
                                activeAction = AdminActionSpec(
                                    title = "Destek Talebini Kapat",
                                    operation = "admin.support.resolve",
                                    payload = apiPayload("requestId" to req.s("id"), "status" to "rejected"),
                                    field = "response",
                                    label = "Kapanış gerekçesi",
                                    minimum = 3,
                                    destructive = true
                                )
                            }
                        )
                    }
                }

                // 1. MODERASYON: Şikayet Bildirimleri + Düzenlenmiş / Bekleyen Anılar
                1 -> {
                    item { AdminSectionHeader("Şikâyet ve İhlal Bildirimleri", reports.size, Icons.Rounded.Flag) }
                    if (reports.isEmpty() && !loading) {
                        item { AdminEmptyState("Açık şikâyet bildirimi bulunmuyor.") }
                    }
                    items(reports, key = { "rep_${it.s("id")}" }) { report ->
                        AdminReportCard(
                            report = report,
                            onZoomPhoto = { previewUploadId = it },
                            onResolve = {
                                activeAction = AdminActionSpec(
                                    title = "Şikâyeti Onayla & İşleme Al",
                                    operation = "admin.report.resolve",
                                    payload = apiPayload("reportId" to report.s("id"), "status" to "resolved"),
                                    label = "Karar gerekçesi",
                                    minimum = 10,
                                    presetText = "Bildirilen içerik incelendi ve moderasyon işlemi uygulandı."
                                )
                            },
                            onDismissReport = {
                                activeAction = AdminActionSpec(
                                    title = "Şikâyeti Reddet (İhlal Yok)",
                                    operation = "admin.report.resolve",
                                    payload = apiPayload("reportId" to report.s("id"), "status" to "dismissed"),
                                    label = "Karar gerekçesi",
                                    minimum = 10,
                                    presetText = "İçerik incelendi ancak topluluk kurallarına aykırılık görülmedi.",
                                    destructive = true
                                )
                            },
                            onRejectMemory = { memoryId ->
                                activeAction = AdminActionSpec(
                                    title = "Bildirilen Anıyı Yayından Kaldır (Düzenlemeye Düşür)",
                                    operation = "admin.memory.review",
                                    payload = apiPayload("memoryId" to memoryId, "approved" to false),
                                    label = "Kullanıcıya iletilecek red gerekçesi",
                                    minimum = 3,
                                    presetText = "Paylaşım topluluk kuralları gereği reddedildi. Düzenleyip yeniden gönderebilirsin.",
                                    destructive = true
                                )
                            },
                            onDeleteMemory = { memoryId ->
                                activeAction = AdminActionSpec(
                                    title = "Anıyı Kalıcı Olarak Sil",
                                    operation = "memory.delete",
                                    payload = apiPayload("memoryId" to memoryId),
                                    field = null,
                                    message = "Bu işlem geri alınamaz. Paylaşım ve konumu tamamen kaldırılır.",
                                    destructive = true
                                )
                            },
                            onDeleteMessage = { msgId, version ->
                                activeAction = AdminActionSpec(
                                    title = "Mesajı Herkesten Sil",
                                    operation = "message.delete",
                                    payload = apiPayload("messageId" to msgId, "expectedVersion" to version),
                                    field = null,
                                    message = "Bildirilen sohbet mesajı tüm katılımcılardan kaldırılır.",
                                    destructive = true
                                )
                            },
                            onSuspendOwner = { ownerId ->
                                activeAction = AdminActionSpec(
                                    title = "İçerik Sahibini Askıya Al",
                                    operation = "admin.user.status",
                                    payload = apiPayload("userId" to ownerId, "status" to "suspended"),
                                    label = "Askıya alma gerekçesi",
                                    minimum = 10,
                                    presetText = "Topluluk kurallarını ihlal eden içerik nedeniyle geçici askıya alındı.",
                                    destructive = true
                                )
                            },
                            onRemoveContent = {
                                activeAction = AdminActionSpec(
                                    title = "Bildirilen İçeriği Kaldır",
                                    operation = "admin.content.remove",
                                    payload = apiPayload("reportId" to report.s("id")),
                                    label = "Moderasyon gerekçesi",
                                    minimum = 10,
                                    presetText = "Topluluk kurallarına aykırı içerik moderasyon ekibince kaldırıldı.",
                                    destructive = true
                                )
                            }
                        )
                    }

                    item { AdminSectionHeader("Onay Bekleyen Anılar (Düzenlenenler)", memories.size, Icons.Rounded.PhotoCamera) }
                    if (memories.isEmpty() && !loading) {
                        item { AdminEmptyState("İnceleme bekleyen saha anısı yok.") }
                    }
                    items(memories, key = { "mem_${it.s("id")}" }) { memory ->
                        AdminPendingMemoryCard(
                            memory = memory,
                            onZoomPhoto = { previewUploadId = it },
                            onApprove = {
                                activeAction = AdminActionSpec(
                                    title = "Anıyı Onayla & Yayınla",
                                    operation = "admin.memory.review",
                                    payload = apiPayload("memoryId" to memory.s("id"), "approved" to true),
                                    label = "Karar notu",
                                    minimum = 3,
                                    presetText = "Paylaşım kurallara uygun görülerek yayınlandı."
                                )
                            },
                            onReject = {
                                activeAction = AdminActionSpec(
                                    title = "Anıyı Reddet",
                                    operation = "admin.memory.review",
                                    payload = apiPayload("memoryId" to memory.s("id"), "approved" to false),
                                    label = "Red gerekçesi",
                                    minimum = 3,
                                    presetText = "Görsel veya açıklama topluluk kurallarına uygun değil.",
                                    destructive = true
                                )
                            },
                            onDelete = {
                                activeAction = AdminActionSpec(
                                    title = "Anıyı Kalıcı Sil",
                                    operation = "memory.delete",
                                    payload = apiPayload("memoryId" to memory.s("id")),
                                    field = null,
                                    message = "Paylaşım kalıcı olarak silinir.",
                                    destructive = true
                                )
                            }
                        )
                    }
                }

                // 2. TESİS & MAÇ: Başvurular, Değerlendirmeler, Skor İtirazları
                2 -> {
                    item {
                        MButton("Tesis Operatör Panelini Aç", onOpenVenueOperator, icon = Icons.Rounded.Storefront, isPrimary = false)
                    }
                    item { AdminSectionHeader("Tesis İşletmeci Başvuruları", venueApps.size, Icons.Rounded.AddBusiness) }
                    if (venueApps.isEmpty() && !loading) {
                        item { AdminEmptyState("Bekleyen tesis başvurusu yok.") }
                    }
                    items(venueApps, key = { "vapp_${it.s("id")}" }) { app ->
                        AdminVenueApplicationCard(
                            app = app,
                            onZoomPhoto = { previewUploadId = it },
                            onApprove = {
                                activeAction = AdminActionSpec(
                                    title = "Tesis Başvurusunu Onayla",
                                    operation = "admin.venue.application.review",
                                    payload = apiPayload("applicationId" to app.s("id"), "expectedVersion" to app.i("version", 1), "approve" to true),
                                    field = "response",
                                    label = "Onay yanıtı",
                                    minimum = 3,
                                    presetText = "Tesis bilgileriniz doğrulandı ve yayına alındı."
                                )
                            },
                            onReject = {
                                activeAction = AdminActionSpec(
                                    title = "Tesis Başvurusunu Reddet",
                                    operation = "admin.venue.application.review",
                                    payload = apiPayload("applicationId" to app.s("id"), "expectedVersion" to app.i("version", 1), "approve" to false),
                                    field = "response",
                                    label = "Red yanıtı",
                                    minimum = 3,
                                    presetText = "Tesis adresi veya doğrulama bilgileri eksik.",
                                    destructive = true
                                )
                            }
                        )
                    }

                    item { AdminSectionHeader("Bekleyen Tesis Değerlendirmeleri", reviews.size, Icons.Rounded.RateReview) }
                    if (reviews.isEmpty() && !loading) {
                        item { AdminEmptyState("Onay bekleyen tesis değerlendirmesi yok.") }
                    }
                    items(reviews, key = { "rev_${it.s("id")}" }) { review ->
                        AdminVenueReviewCard(
                            review = review,
                            onApprove = {
                                activeAction = AdminActionSpec(
                                    title = "Değerlendirmeyi Yayınla",
                                    operation = "admin.venue.review",
                                    payload = apiPayload("reviewId" to review.s("id"), "approved" to true),
                                    label = "Karar gerekçesi",
                                    minimum = 10,
                                    presetText = "Doğrulanmış rezervasyon değerlendirmesi yayınlandı."
                                )
                            },
                            onReject = {
                                activeAction = AdminActionSpec(
                                    title = "Değerlendirmeyi Reddet",
                                    operation = "admin.venue.review",
                                    payload = apiPayload("reviewId" to review.s("id"), "approved" to false),
                                    label = "Red gerekçesi",
                                    minimum = 10,
                                    presetText = "Yorum içerik standartlarına uygun bulunmadı.",
                                    destructive = true
                                )
                            }
                        )
                    }

                    item { AdminSectionHeader("Maç Sonucu & Skor İtirazları", results.size, Icons.Rounded.Gavel) }
                    if (results.isEmpty() && !loading) {
                        item { AdminEmptyState("İnceleme bekleyen skor veya itiraz yok.") }
                    }
                    items(results, key = { "res_${it.s("event_id")}" }) { result ->
                        AdminResultCard(
                            result = result,
                            onApprove = {
                                activeAction = AdminActionSpec(
                                    title = "Skoru Onayla & Sonuçlandır",
                                    operation = "admin.result.resolve",
                                    payload = apiPayload("eventId" to result.s("event_id"), "approve" to true),
                                    label = "İnceleme gerekçesi",
                                    minimum = 10,
                                    presetText = "Katılım ve skor kayıtları operatör tarafından doğrulandı."
                                )
                            },
                            onReject = {
                                activeAction = AdminActionSpec(
                                    title = "Skoru Reddet (Geçersiz Say)",
                                    operation = "admin.result.resolve",
                                    payload = apiPayload("eventId" to result.s("event_id"), "approve" to false),
                                    label = "Red gerekçesi",
                                    minimum = 10,
                                    presetText = "Skor kanıtları ve taraf beyanları uyuşmuyor.",
                                    destructive = true
                                )
                            }
                        )
                    }
                }

                // 3. SİSTEM: Kullanıcılar, Özellik Anahtarları, Duyuru, Yasal, Kuyruk & Denetim
                3 -> {
                    item { AdminSectionHeader("Kullanıcı ve Yetki Yönetimi", users.size, Icons.Rounded.ManageAccounts) }
                    item {
                        MCard {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    label = { Text("Ad, telefon (+90...), e-posta veya ID ara") },
                                    singleLine = true,
                                    trailingIcon = {
                                        IconButton(onClick = { localRefresh++ }) {
                                            Icon(Icons.Rounded.Search, "Ara")
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                MPillActionButton("Listeyi Ara / Yenile", { localRefresh++ }, icon = Icons.Rounded.Search, tone = MPillTone.BRAND, compact = true)
                            }
                        }
                    }
                    items(users, key = { "usr_${it.s("id")}" }) { u ->
                        val isSelf = u.s("id") == wire?.s("userId")
                        val userStatus = u.s("status")
                        val userRole = u.s("role")
                        MCard {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(u.s("name").ifBlank { "Yeni hesap" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    AdminTagChip(userRole.uppercase(), if (userRole == "admin") MColors.BrandDeep else MColors.Ink700)
                                }
                                val contactLine = listOfNotNull(
                                    u.s("phone").takeIf { it.isNotBlank() },
                                    u.s("email").takeIf { it.isNotBlank() },
                                    listOf(u.s("city"), u.s("area")).filter { it.isNotBlank() }.joinToString(" · ").takeIf { it.isNotBlank() }
                                ).joinToString(" · ")
                                if (contactLine.isNotBlank()) Text(contactLine, style = MaterialTheme.typography.bodySmall, color = MColors.Ink600)
                                Text("Durum: ${if (userStatus == "suspended") "Askıda" else "Aktif"} · ID: ${u.s("id").take(8)}...", style = MaterialTheme.typography.labelSmall, color = if (userStatus == "suspended") MColors.Rose else MColors.Mint)
                                if (!isSelf && userRole != "admin") {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        val nextStatus = if (userStatus == "suspended") "active" else "suspended"
                                        MPillActionButton(
                                            text = if (userStatus == "suspended") "Hesabı Aç" else "Askıya Al",
                                            tone = if (userStatus == "suspended") MPillTone.BRAND else MPillTone.DANGER,
                                            compact = true,
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                activeAction = AdminActionSpec(
                                                    title = if (nextStatus == "suspended") "Kullanıcıyı Askıya Al" else "Kullanıcıyı Aktif Et",
                                                    operation = "admin.user.status",
                                                    payload = apiPayload("userId" to u.s("id"), "status" to nextStatus),
                                                    label = "Gerekçe",
                                                    minimum = 10,
                                                    destructive = nextStatus == "suspended"
                                                )
                                            }
                                        )
                                        if (userStatus == "active" && userRole == "venue_owner") {
                                            MPillActionButton(
                                                text = "Tesis Yetkisini Al",
                                                tone = MPillTone.NEUTRAL,
                                                compact = true,
                                                modifier = Modifier.weight(1f),
                                                onClick = {
                                                    activeAction = AdminActionSpec(
                                                        title = "Tesis Yetkisini Kaldır",
                                                        operation = "admin.operator.assign",
                                                        payload = apiPayload("userId" to u.s("id"), "role" to "user"),
                                                        label = "Gerekçe",
                                                        minimum = 10,
                                                        destructive = true
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item { AdminSectionHeader("Özellik Anahtarları (Kill Switch)", featureFlags.size, Icons.Rounded.ToggleOn) }
                    item {
                        MCard {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                featureFlags.forEach { flagRow ->
                                    val flagName = flagRow.s("name")
                                    val enabled = flagRow.i("enabled", 1) != 0 && flagRow.b("enabled", true)
                                    val label = when (flagName) {
                                        "billing" -> "Ödeme ve Plus Mağazası"
                                        "chat" -> "Sohbet & Mesajlaşma"
                                        "memories" -> "Saha Anıları Vitrini"
                                        "tournaments" -> "Turnuvalar"
                                        "venues" -> "Tesis & Rezervasyon"
                                        else -> flagName
                                    }
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(Modifier.weight(1f)) {
                                            Text(label, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                            Text(if (enabled) "Yayında" else "Devre dışı (503)", style = MaterialTheme.typography.bodySmall, color = if (enabled) MColors.Mint else MColors.Rose)
                                        }
                                        Switch(
                                            checked = enabled,
                                            onCheckedChange = { target ->
                                                scope.launch {
                                                    try {
                                                        backend.command("admin.feature.set", apiPayload("name" to flagName, "enabled" to target), backend.requestKey("flag-$flagName"))
                                                        toast?.invoke("$label ${if (target) "açıldı" else "kapatıldı"}", if (target) "✓" else "⚠️")
                                                        localRefresh++
                                                    } catch (cancel: CancellationException) { throw cancel }
                                                    catch (e: Exception) { toast?.invoke((e as? BackendFailure)?.message ?: "Ayar değiştirilemedi.", "⚠️") }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item { AdminSectionHeader("Pazarlama & Sistem Duyurusu", announcements.size, Icons.Rounded.Campaign) }
                    item {
                        AdminAnnouncementCard(
                            announcements = announcements,
                            onSend = { title, body, city ->
                                scope.launch {
                                    try {
                                        val res = backend.command("admin.announcement.send", apiPayload("title" to title, "body" to body, "city" to city), backend.requestKey("announce"))
                                        toast?.invoke("Duyuru ${res.i("recipients")} kullanıcıya iletildi", "📢")
                                        localRefresh++
                                    } catch (cancel: CancellationException) { throw cancel }
                                    catch (e: Exception) { toast?.invoke((e as? BackendFailure)?.message ?: "Duyuru gönderilemedi.", "⚠️") }
                                }
                            }
                        )
                    }

                    item { AdminSectionHeader("Yasal Belgeler", 0, Icons.Rounded.Description) }
                    item {
                        AdminLegalDocumentsCard(legalDocs = legalDocs, onPublished = { localRefresh++ })
                    }

                    if (outboxFailures.isNotEmpty() || identityDeletions.isNotEmpty() || deletionRequests.isNotEmpty()) {
                        item { AdminSectionHeader("Kuyruk & Hesap Temizleme", badgeTab3, Icons.Rounded.SyncProblem) }
                        if (outboxFailures.isNotEmpty()) {
                            item {
                                MButton("Başarısız Tüm Bildirimleri Yeniden Dene (${outboxFailures.size})", {
                                    activeAction = AdminActionSpec(
                                        title = "Başarısız Bildirimleri Yeniden Kuyruğa Al",
                                        operation = "admin.outbox.retry_all",
                                        payload = apiPayload(),
                                        field = null,
                                        message = "Başarısız durumundaki tüm push bildirimler sıfırlanarak yeniden gönderim sırasına alınır."
                                    )
                                }, icon = Icons.Rounded.Refresh, isPrimary = false)
                            }
                        }
                        items(outboxFailures, key = { "out_${it.s("id")}" }) { job ->
                            MCard {
                                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text("${job.s("recipientName").ifBlank { job.s("user_id") }} · ${job.s("title").ifBlank { job.s("kind") }}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text("Deneme: ${job.i("attempts")} · Hedef: ${job.s("destination")}", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
                                    }
                                    TextButton(onClick = {
                                        activeAction = AdminActionSpec(
                                            title = "Bildirimi Yeniden Dene",
                                            operation = "admin.outbox.retry",
                                            payload = apiPayload("outboxId" to job.s("id")),
                                            field = null
                                        )
                                    }) { Text("Tekrar dene") }
                                }
                            }
                        }
                        items(identityDeletions, key = { "idel_${it.s("id")}" }) { item ->
                            MCard {
                                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text("Kimlik silme · ${item.s("id").take(12)}...", fontWeight = FontWeight.Bold)
                                        Text("Durum: ${item.s("status")} · Deneme: ${item.i("attempts")} · ${item.s("last_error")}", style = MaterialTheme.typography.bodySmall, color = MColors.Rose)
                                    }
                                    TextButton(onClick = {
                                        activeAction = AdminActionSpec(
                                            title = "Kimlik Silmeyi Yeniden Dene",
                                            operation = "admin.identity.cleanup.retry",
                                            payload = apiPayload("userId" to item.s("id")),
                                            field = null
                                        )
                                    }) { Text("Tekrar dene") }
                                }
                            }
                        }
                        items(deletionRequests, key = { "delreq_${it.s("userId")}" }) { req ->
                            MCard {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text("Bekleyen hesap silme talebi · ${req.s("name")}", fontWeight = FontWeight.Bold)
                                    Text("İnfaz: ${formatAdminTimestamp(req.l("executeAfter"))} · ${req.s("phone")}", style = MaterialTheme.typography.bodySmall, color = MColors.Ink600)
                                }
                            }
                        }
                    }

                    if (recentAudit.isNotEmpty()) {
                        item { AdminSectionHeader("Son Yönetim İşlemleri", recentAudit.size, Icons.Rounded.History) }
                        items(recentAudit.take(15), key = { "aud_${it.s("id")}" }) { aud ->
                            MSubCard {
                                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("${aud.s("actorName")} · ${aud.s("action")}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                                        if (aud.s("summary").isNotBlank()) {
                                            Text(aud.s("summary"), style = MaterialTheme.typography.labelSmall, color = MColors.Ink600, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                    Text(formatAdminTimestamp(aud.l("createdAt")), style = MaterialTheme.typography.labelSmall, color = MColors.Ink400)
                                }
                            }
                        }
                    }

                    item {
                        MCard {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Yönetici Hesabı", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    MPillActionButton(
                                        text = "Veri Dışa Aktar",
                                        icon = Icons.Rounded.Download,
                                        tone = MPillTone.NEUTRAL,
                                        compact = true,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            scope.launch {
                                                runCatching { shareAccountExport(backend.exportAccount().toString()) }
                                                    .onSuccess { toast?.invoke("Veri dışa aktarıldı", "✓") }
                                                    .onFailure { toast?.invoke((it as? BackendFailure)?.message ?: "Dışa aktarılamadı.", "⚠️") }
                                            }
                                        }
                                    )
                                    MPillActionButton(
                                        text = "Çıkış Yap",
                                        icon = Icons.AutoMirrored.Rounded.Logout,
                                        tone = MPillTone.DANGER,
                                        compact = true,
                                        modifier = Modifier.weight(1f),
                                        onClick = { confirmLogout = true }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    activeAction?.let { spec ->
        AdminDecisionDialog(
            action = spec,
            onSuccess = {
                activeAction = null
                localRefresh++
            },
            onDismiss = { activeAction = null }
        )
    }

    previewUploadId?.let { uid ->
        Dialog(onDismissRequest = { previewUploadId = null }) {
            Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFF0B1220)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Tam Görsel İnceleme", color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { previewUploadId = null }) { Icon(Icons.Rounded.Close, "Kapat", tint = Color.White) }
                    }
                    Box(Modifier.fillMaxWidth().height(380.dp).clip(RoundedCornerShape(12.dp))) {
                        BackendPhoto(uid, "Tam boyutlu görsel", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    }
                }
            }
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Yönetici oturumu kapatılsın mı?") },
            text = { Text("Çıkış yaptığında uygulama standart giriş/misafir ekranına döner.") },
            confirmButton = { TextButton(onClick = { confirmLogout = false; onLogout() }) { Text("Çıkış Yap", color = MColors.Rose) } },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Vazgeç") } }
        )
    }
}

@Composable
private fun AdminTopBar(
    totalPending: Int,
    unreadNotifications: Int,
    loading: Boolean,
    onRefresh: () -> Unit,
    onNotifications: () -> Unit,
    onVenueOperator: () -> Unit,
    onLogout: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                    Surface(shape = RoundedCornerShape(12.dp), color = MColors.BrandDeep, modifier = Modifier.size(38.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.AdminPanelSettings, null, tint = Color.White, modifier = Modifier.size(21.dp))
                        }
                    }
                    Column {
                        Text("Matchora Yönetim Paneli", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MColors.Ink900, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            if (loading) "Veriler güncelleniyor…" else "$totalPending bekleyen işlem",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (totalPending > 0) MColors.Rose else MColors.Mint,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onVenueOperator) { Icon(Icons.Rounded.Storefront, "Tesisler", tint = MColors.Ink700) }
                    BadgedBox(
                        badge = {
                            if (unreadNotifications > 0) {
                                Badge(containerColor = MColors.Rose, contentColor = Color.White) {
                                    Text(if (unreadNotifications > 99) "99+" else "$unreadNotifications", fontSize = 9.sp)
                                }
                            }
                        }
                    ) {
                        IconButton(onClick = onNotifications) { Icon(Icons.Rounded.Notifications, "Bildirimler", tint = MColors.Ink700) }
                    }
                    IconButton(onClick = onRefresh) { Icon(Icons.Rounded.Refresh, "Yenile", tint = MColors.Ink700) }
                    IconButton(onClick = onLogout) { Icon(Icons.AutoMirrored.Rounded.Logout, "Çıkış", tint = MColors.Rose) }
                }
            }
        }
    }
}

@Composable
private fun AdminBottomBar(selectedTab: Int, onSelectTab: (Int) -> Unit, badges: List<Int>) {
    val items = listOf(
        Triple("Talepler", Icons.Rounded.VerifiedUser, badges.getOrElse(0) { 0 }),
        Triple("Moderasyon", Icons.Rounded.Flag, badges.getOrElse(1) { 0 }),
        Triple("Tesis & Maç", Icons.Rounded.Stadium, badges.getOrElse(2) { 0 }),
        Triple("Sistem", Icons.Rounded.Tune, badges.getOrElse(3) { 0 })
    )
    Surface(color = Color.White, shadowElevation = 8.dp, border = BorderStroke(1.dp, Color(0xFFE2E8F0))) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(vertical = 6.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items.forEachIndexed { index, (label, icon, badge) ->
                val selected = selectedTab == index
                val tint = if (selected) MColors.BrandDeep else MColors.Ink500
                Column(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { onSelectTab(index) }.padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    BadgedBox(
                        badge = {
                            if (badge > 0) Badge(containerColor = MColors.Rose, contentColor = Color.White) {
                                Text(if (badge > 99) "99+" else "$badge", fontSize = 9.sp)
                            }
                        }
                    ) {
                        Icon(icon, label, tint = tint, modifier = Modifier.size(21.dp))
                    }
                    Text(label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = tint, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun AdminQueueStrip(
    photos: Int, support: Int, reports: Int, memories: Int, venueApps: Int, reviews: Int, results: Int, failures: Int,
    onJump: (Int) -> Unit
) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AdminStatChip("Fotoğraf", photos, 0, onJump)
        AdminStatChip("Destek", support, 0, onJump)
        AdminStatChip("Şikâyet", reports, 1, onJump)
        AdminStatChip("Anı", memories, 1, onJump)
        AdminStatChip("Tesis Başv.", venueApps, 2, onJump)
        AdminStatChip("Yorum", reviews, 2, onJump)
        AdminStatChip("Skor İtiraz", results, 2, onJump)
        AdminStatChip("Hata/Kuyruk", failures, 3, onJump)
    }
}

@Composable
private fun AdminStatChip(label: String, count: Int, tab: Int, onClick: (Int) -> Unit) {
    Surface(
        onClick = { onClick(tab) },
        shape = RoundedCornerShape(12.dp),
        color = if (count > 0) Color(0xFFFFF7ED) else Color.White,
        border = BorderStroke(1.dp, if (count > 0) MColors.Brand.copy(alpha = 0.45f) else Color(0xFFE2E8F0))
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MColors.Ink700)
            Surface(shape = CircleShape, color = if (count > 0) MColors.BrandDeep else Color(0xFFE2E8F0)) {
                Text("$count", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (count > 0) Color.White else MColors.Ink700, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
    }
}

@Composable
private fun AdminSectionHeader(title: String, count: Int, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
        Icon(icon, null, tint = MColors.BrandDeep, modifier = Modifier.size(18.dp))
        Text("$title ($count)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MColors.Ink900)
    }
}

@Composable
private fun AdminEmptyState(text: String) {
    MSubCard {
        Text(text, style = MaterialTheme.typography.bodySmall, color = MColors.Ink500, modifier = Modifier.padding(14.dp))
    }
}

@Composable
private fun AdminTagChip(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.12f)) {
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

@Composable
private fun AdminPhotoVerificationCard(
    req: JsonObject,
    onZoomPhoto: (String) -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val uploadId = req.s("uploadId")
    val stale = req.b("stale")
    MCard {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(req.s("requesterName").ifBlank { "Sporcu" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    val meta = listOf(req.s("requesterCity"), req.s("requesterArea"), req.s("requesterPhone")).filter { it.isNotBlank() }.joinToString(" · ")
                    if (meta.isNotBlank()) Text(meta, style = MaterialTheme.typography.bodySmall, color = MColors.Ink600)
                }
                AdminTagChip(if (stale) "ESKİ FOTOĞRAF" else "GÜNCEL", if (stale) MColors.Rose else MColors.Mint)
            }
            if (uploadId.isNotBlank()) {
                Box(
                    Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(14.dp)).clickable { onZoomPhoto(uploadId) }
                ) {
                    BackendPhoto(uploadId, "Doğrulama fotoğrafı", Modifier.fillMaxSize())
                    Surface(shape = RoundedCornerShape(8.dp), color = Color.Black.copy(alpha = 0.55f), modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)) {
                        Text("🔍 Büyüt", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }
            if (stale) {
                Text("Kullanıcı bu talepten sonra profil fotoğrafını değiştirmiş. Onay verilemez; kuyruğu temizlemek için reddedebilirsin.", style = MaterialTheme.typography.bodySmall, color = MColors.Rose)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MPillActionButton("Onayla (PHOTO)", onApprove, icon = Icons.Rounded.Verified, tone = MPillTone.BRAND, compact = true, enabled = !stale && uploadId.isNotBlank(), modifier = Modifier.weight(1f))
                MPillActionButton("Reddet", onReject, icon = Icons.Rounded.Close, tone = MPillTone.DANGER, compact = true, enabled = uploadId.isNotBlank(), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AdminSupportCard(req: JsonObject, onResolve: () -> Unit, onReject: () -> Unit) {
    MCard {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(req.s("subject"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                AdminTagChip(req.s("category"), MColors.BrandDeep)
            }
            Text("${req.s("requesterName").ifBlank { "Sporcu" }} ${req.s("requesterPhone").takeIf { it.isNotBlank() }?.let { "· $it" }.orEmpty()}", style = MaterialTheme.typography.labelSmall, color = MColors.Ink600)
            Text(req.s("message"), style = MaterialTheme.typography.bodySmall, color = MColors.Ink700)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MPillActionButton("Yanıtla & Çöz", onResolve, icon = Icons.Rounded.Check, tone = MPillTone.BRAND, compact = true, modifier = Modifier.weight(1f))
                MPillActionButton("Reddet", onReject, icon = Icons.Rounded.Close, tone = MPillTone.DANGER, compact = true, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AdminReportCard(
    report: JsonObject,
    onZoomPhoto: (String) -> Unit,
    onResolve: () -> Unit,
    onDismissReport: () -> Unit,
    onRejectMemory: (String) -> Unit,
    onDeleteMemory: (String) -> Unit,
    onDeleteMessage: (String, Int) -> Unit,
    onSuspendOwner: (String) -> Unit,
    onRemoveContent: (() -> Unit)? = null
) {
    val context = report.o("context")
    val targetType = report.s("target_type")
    val targetId = report.s("target_id")
    val uploadId = report.s("targetUploadId")
    val ownerId = report.s("targetOwnerId")
    MCard {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(report.s("reason"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                AdminTagChip(targetType.uppercase(), MColors.Rose)
            }
            Text("Bildiren: ${report.s("reporterName").ifBlank { "Sporcu" }}", style = MaterialTheme.typography.labelSmall, color = MColors.Ink600)
            if (report.s("details").isNotBlank()) Text(report.s("details"), style = MaterialTheme.typography.bodySmall, color = MColors.Ink700)
            if (context.isNotEmpty()) {
                MSubCard {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(context.s("title").ifBlank { "Bildirilen kayıt" }, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        if (context.s("description").isNotBlank()) Text(context.s("description"), style = MaterialTheme.typography.bodySmall, color = MColors.Ink600)
                        if (context.s("status").isNotBlank()) Text("Durum: ${context.s("status")}", style = MaterialTheme.typography.labelSmall, color = MColors.Ink500)
                    }
                }
            }
            if (uploadId.isNotBlank()) {
                Box(Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp)).clickable { onZoomPhoto(uploadId) }) {
                    BackendPhoto(uploadId, "Bildirilen içerik görseli", Modifier.fillMaxSize())
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MPillActionButton("Şikâyeti Onayla", onResolve, icon = Icons.Rounded.Gavel, tone = MPillTone.BRAND, compact = true, modifier = Modifier.weight(1f))
                MPillActionButton("İhlal Yok (Kapat)", onDismissReport, icon = Icons.Rounded.Close, tone = MPillTone.NEUTRAL, compact = true, modifier = Modifier.weight(1f))
            }
            if (targetType == "memory" || targetType == "message" || ownerId.isNotBlank() || onRemoveContent != null) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (targetType == "memory") {
                        if (context.s("status") != "rejected") {
                            MPillActionButton("Anıyı Reddet", { onRejectMemory(targetId) }, tone = MPillTone.DANGER, compact = true)
                        }
                        MPillActionButton("Anıyı Kalıcı Sil", { onDeleteMemory(targetId) }, tone = MPillTone.DANGER, compact = true)
                    }
                    if (targetType == "message" && !report.b("targetDeleted")) {
                        MPillActionButton("Mesajı Sil", { onDeleteMessage(targetId, report.i("targetVersion", 1)) }, tone = MPillTone.DANGER, compact = true)
                    }
                    if (onRemoveContent != null && targetType in setOf("event", "team", "venue", "venue_review", "thread")) {
                        MPillActionButton("İçeriği Kaldır", onRemoveContent, tone = MPillTone.DANGER, compact = true)
                    }
                    if (ownerId.isNotBlank()) {
                        MPillActionButton("Hesabı Askıya Al", { onSuspendOwner(ownerId) }, tone = MPillTone.DANGER, compact = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminPendingMemoryCard(
    memory: JsonObject,
    onZoomPhoto: (String) -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onDelete: () -> Unit
) {
    val uploadId = memory.s("upload_id")
    MCard {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${memory.s("authorName").ifBlank { "Sporcu" }} · ${memory.s("sport")}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                AdminTagChip("TEKRAR İNCELEME", MColors.Amber)
            }
            if (memory.s("caption").isNotBlank()) Text(memory.s("caption"), style = MaterialTheme.typography.bodySmall)
            Text("${memory.s("venue")} · ${memory.s("city")} / ${memory.s("area")}", style = MaterialTheme.typography.labelSmall, color = MColors.Ink500)
            if (uploadId.isNotBlank()) {
                Box(Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(12.dp)).clickable { onZoomPhoto(uploadId) }) {
                    BackendPhoto(uploadId, "Anı fotoğrafı", Modifier.fillMaxSize())
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MPillActionButton("Onayla", onApprove, icon = Icons.Rounded.Check, tone = MPillTone.BRAND, compact = true, modifier = Modifier.weight(1f))
                MPillActionButton("Reddet", onReject, icon = Icons.Rounded.Close, tone = MPillTone.DANGER, compact = true, modifier = Modifier.weight(1f))
                MPillActionButton("Sil", onDelete, icon = Icons.Rounded.DeleteOutline, tone = MPillTone.DANGER, compact = true)
            }
        }
    }
}

@Composable
private fun AdminVenueApplicationCard(
    app: JsonObject,
    onZoomPhoto: (String) -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val photos = app.ss("photoUploadIds").take(4)
    MCard {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(app.s("name"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text("Başvuran: ${app.s("applicantName")} ${app.s("applicantPhone").takeIf { it.isNotBlank() }?.let { "· $it" }.orEmpty()}", style = MaterialTheme.typography.labelSmall, color = MColors.Ink600)
            Text("${app.s("city")} · ${app.s("area")} · ${app.i("courts", 1)} saha · ${app.i("cancellationHours", 24)}s iptal", style = MaterialTheme.typography.bodySmall)
            if (app.s("address").isNotBlank()) Text(app.s("address"), style = MaterialTheme.typography.bodySmall, color = MColors.Ink600)
            if (app.s("description").isNotBlank()) Text(app.s("description"), style = MaterialTheme.typography.bodySmall, color = MColors.Ink600)
            if (photos.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(photos) { idx, uid ->
                        Box(Modifier.size(width = 104.dp, height = 78.dp).clip(RoundedCornerShape(10.dp)).clickable { onZoomPhoto(uid) }) {
                            BackendPhoto(uid, "Tesis fotoğrafı ${idx + 1}", Modifier.fillMaxSize())
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MPillActionButton("Onayla & Yayınla", onApprove, icon = Icons.Rounded.Check, tone = MPillTone.BRAND, compact = true, modifier = Modifier.weight(1f))
                MPillActionButton("Reddet", onReject, icon = Icons.Rounded.Close, tone = MPillTone.DANGER, compact = true, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AdminVenueReviewCard(review: JsonObject, onApprove: () -> Unit, onReject: () -> Unit) {
    MCard {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(review.s("venueName").ifBlank { "Tesis" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                AdminTagChip("${review.i("rating", 5)}/5 ★", MColors.BrandDeep)
            }
            Text("Yazan: ${review.s("reviewerName").ifBlank { "Sporcu" }}", style = MaterialTheme.typography.labelSmall, color = MColors.Ink600)
            if (review.s("comment").isNotBlank()) Text(review.s("comment"), style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MPillActionButton("Yayınla", onApprove, icon = Icons.Rounded.Check, tone = MPillTone.BRAND, compact = true, modifier = Modifier.weight(1f))
                MPillActionButton("Reddet", onReject, icon = Icons.Rounded.Close, tone = MPillTone.DANGER, compact = true, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AdminResultCard(result: JsonObject, onApprove: () -> Unit, onReject: () -> Unit) {
    MCard {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(result.s("eventTitle").ifBlank { "Maç" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                AdminTagChip("${result.i("homeScore")} – ${result.i("awayScore")}", MColors.Ink900)
            }
            Text("Bildiren: ${result.s("submitterName")} · Durum: ${result.s("status")}", style = MaterialTheme.typography.labelSmall, color = MColors.Ink600)
            if (result.s("disputeReason").isNotBlank()) {
                MSubCard {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("İtiraz ${result.s("disputedByName").takeIf { it.isNotBlank() }?.let { "($it)" }.orEmpty()}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, color = MColors.Rose)
                        Text(result.s("disputeReason"), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MPillActionButton("Skoru Onayla", onApprove, icon = Icons.Rounded.Verified, tone = MPillTone.BRAND, compact = true, modifier = Modifier.weight(1f))
                MPillActionButton("Skoru Reddet", onReject, icon = Icons.Rounded.Close, tone = MPillTone.DANGER, compact = true, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AdminAnnouncementCard(announcements: List<JsonObject>, onSend: (String, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var showConfirm by remember { mutableStateOf(false) }
    MCard {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MPremiumTextField(title, { title = it.take(120) }, label = "Duyuru başlığı")
            MPremiumTextField(body, { body = it.take(1000) }, label = "Duyuru içeriği (en az 10 karakter)", singleLine = false, minLines = 3, maxLines = 5)
            MPremiumTextField(city, { city = it.take(60) }, label = "Şehir filtresi (tümü için boş bırak)")
            MButton("Duyuru Gönder", { showConfirm = true }, enabled = title.trim().length >= 3 && body.trim().length >= 10, icon = Icons.Rounded.Campaign)
            if (announcements.isNotEmpty()) {
                Text("Son duyuru: ${announcements.first().i("recipients")} alıcı · ${formatAdminTimestamp(announcements.first().l("createdAt"))}", style = MaterialTheme.typography.labelSmall, color = MColors.Ink500)
            }
        }
    }
    if (showConfirm) {
        val targetCity = city.trim().ifBlank { "Tüm kullanıcılar" }
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Duyuruyu Gönder?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Hedef: $targetCity", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                    Text("Başlık: ${title.trim()}", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodySmall)
                    Text(body.trim(), style = MaterialTheme.typography.bodySmall, color = MColors.Ink600)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val t = title.trim(); val b = body.trim(); val c = city.trim()
                    showConfirm = false
                    onSend(t, b, c)
                    title = ""; body = ""; city = ""
                }) { Text("Gönder") }
            },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Vazgeç") } }
        )
    }
}

@Composable
private fun AdminLegalDocumentsCard(legalDocs: JsonObject, onPublished: () -> Unit) {
    val backend = LocalBackendRepository.current ?: return
    val toast = LocalToastNotifier.current
    val scope = rememberCoroutineScope()
    val docs = legalDocs.o("documents")
    var kind by remember { mutableStateOf("privacy") }
    var version by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    MCard {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("privacy" to "Gizlilik", "terms" to "Koşullar", "guidelines" to "Topluluk", "deletion" to "Veri Silme").forEach { (k, label) ->
                val d = docs.o(k)
                Text("$label: v${d.s("version").ifBlank { "-" }} · ${d.s("url").ifBlank { "Tanımsız" }}", style = MaterialTheme.typography.labelSmall, color = MColors.Ink600, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            MFloatingDropdownBox("Belge türü", kind, listOf("privacy", "terms", "guidelines", "deletion"), {
                when (it) { "privacy" -> "Gizlilik Politikası"; "terms" -> "Kullanım Koşulları"; "guidelines" -> "Topluluk Kuralları"; else -> "Hesap Silme" }
            }, { kind = it })
            MPremiumTextField(version, { version = it.take(80) }, label = "Yeni sürüm (ör. 2026.2)")
            MPremiumTextField(url, { url = it.take(2000) }, label = "HTTPS Bağlantı")
            MButton(if (busy) "Yayınlanıyor…" else "Yeni Sürüm Yayınla", {
                scope.launch {
                    busy = true
                    try {
                        backend.command("admin.legal.publish", apiPayload("kind" to kind, "version" to version.trim(), "url" to url.trim()), backend.requestKey("legal"))
                        toast?.invoke("Yasal belge yayınlandı", "✓")
                        version = ""; url = ""
                        onPublished()
                    } catch (cancel: CancellationException) { throw cancel }
                    catch (e: Exception) { toast?.invoke((e as? BackendFailure)?.message ?: "Yayınlanamadı.", "⚠️") }
                    finally { busy = false }
                }
            }, enabled = !busy && version.isNotBlank() && url.startsWith("https://"))
        }
    }
}

@Composable
private fun AdminDecisionDialog(
    action: AdminActionSpec,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    val backend = LocalBackendRepository.current ?: return
    val toast = LocalToastNotifier.current
    val scope = rememberCoroutineScope()
    var input by remember(action) { mutableStateOf(action.presetText) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val valid = action.field == null || input.trim().length >= action.minimum

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(action.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                action.message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MColors.Ink600) }
                action.field?.let {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it; error = null },
                        label = { Text("${action.label} (en az ${action.minimum} karakter)") },
                        singleLine = false,
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "${input.trim().length}/${action.minimum}+ karakter",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (valid) MColors.Mint else MColors.Rose
                    )
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                error?.let { Text(it, color = MColors.Rose, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy && valid,
                onClick = {
                    scope.launch {
                        busy = true; error = null
                        try {
                            val merged = if (action.field == null) action.payload else JsonObject(action.payload + (action.field to kotlinx.serialization.json.JsonPrimitive(input.trim())))
                            backend.command(action.operation, merged, backend.requestKey("adm"))
                            toast?.invoke("İşlem kaydedildi", "✓")
                            onSuccess()
                        } catch (cancel: CancellationException) {
                            throw cancel
                        } catch (failure: Exception) {
                            error = (failure as? BackendFailure)?.message ?: "İşlem kaydedilemedi."
                        } finally {
                            busy = false
                        }
                    }
                }
            ) {
                Text("Onayla", color = if (action.destructive) MColors.Rose else MColors.BrandDeep, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Vazgeç") } }
    )
}

private fun formatAdminTimestamp(epochMillis: Long): String {
    if (epochMillis <= 0L) return "-"
    val dt = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.of("Europe/Istanbul"))
    return "${dt.dayOfMonth}.${dt.monthNumber} ${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}"
}
