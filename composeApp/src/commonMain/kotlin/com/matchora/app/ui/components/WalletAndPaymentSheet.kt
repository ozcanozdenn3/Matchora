package com.matchora.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchora.app.state.*
import com.matchora.app.data.*
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalUriHandler
import com.matchora.app.ui.theme.MColors

private enum class WalletSheetTab(val label: String) {
    TOKEN_PACKS("Token Paketleri"),
    PLUS_SUBSCRIPTION("Matchora Plus")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletAndPaymentSheet(
    onDismiss: () -> Unit,
    tokenBalance: Int,
    lockedTokens: Int,
    ledger: List<LedgerEntry>,
    paymentHolderName: String,
    activeProPlan: ProPlan? = null,
    initialPlusTab: Boolean = false,
    onSelectTokenPack: (TokenPack) -> Unit,
    onSubscribePlan: (ProPlan) -> Unit = {}
) {
    TokenWalletSheet(
        onDismiss = onDismiss,
        balance = tokenBalance,
        locked = lockedTokens,
        holder = paymentHolderName,
        ledger = ledger,
        activeProPlan = activeProPlan,
        initialPlusTab = initialPlusTab,
        onLoad = onSelectTokenPack,
        onSubscribePlan = onSubscribePlan
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TokenWalletSheet(
    onDismiss: () -> Unit,
    balance: Int,
    locked: Int,
    holder: String,
    ledger: List<LedgerEntry>,
    activeProPlan: ProPlan? = null,
    initialPlusTab: Boolean = false,
    onLoad: (TokenPack) -> Unit,
    onSubscribePlan: (ProPlan) -> Unit = {},
    initialPack: TokenPack = DemoData.tokenPacks.getOrNull(1) ?: DemoData.tokenPacks.first()
) {
    var activeTab by remember { mutableStateOf(if (initialPlusTab) WalletSheetTab.PLUS_SUBSCRIPTION else WalletSheetTab.TOKEN_PACKS) }
    var selectedPack by remember { mutableStateOf(initialPack) }
    var selectedPlanSku by remember { mutableStateOf(DemoData.proPlans.firstOrNull()?.sku ?: "pro_monthly") }
    var loading by remember { mutableStateOf(false) }
    var storeBusy by remember { mutableStateOf(false) }
    var storeNotice by remember { mutableStateOf<String?>(null) }
    var historyFilter by remember { mutableIntStateOf(0) }
    var showHistorySection by remember { mutableStateOf(false) }
    val backend = LocalBackendRepository.current
    val remoteStorePending = backend?.enabled == true
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    var catalog by remember { mutableStateOf(emptyList<StoreListing>()) }
    var catalogError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(backend?.userId) {
        if (remoteStorePending && backend != null) {
            try { catalog = StoreBilling.catalog(backend); catalogError = null }
            catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (e: Exception) { catalogError = (e as? BackendFailure)?.message ?: "Mağaza ürünleri yüklenemedi. Lütfen tekrar dene." }
        }
    }
    fun price(sku: String, preview: String): String = if (!remoteStorePending) preview else catalog.find { it.sku == sku }?.price ?: "Mağaza bekleniyor"
    val storeReady = !remoteStorePending || (backend?.configuration?.o("store")?.b("enabled") == true && catalog.isNotEmpty())
    fun storeAction(sku: String? = null) {
        if (loading || storeBusy || backend == null) return
        if (remoteStorePending && !storeReady) {
            storeNotice = "Mağaza ürünleri henüz aktif değil."
            return
        }
        storeBusy = true; loading = true
        scope.launch {
            try {
                if (catalog.isEmpty()) catalog = StoreBilling.catalog(backend)
                if (sku == null) StoreBilling.restore(backend) else StoreBilling.purchase(backend, sku)
                storeNotice = "Mağaza işlemi alındı. Haklar sunucuda doğrulandığında cüzdanın güncellenecek."
            } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (e: Exception) { if ((e as? BackendFailure)?.code != "PURCHASE_CANCELLED") storeNotice = (e as? BackendFailure)?.message ?: "Mağaza işlemi tamamlanamadı. Lütfen tekrar dene." }
            finally { loading = false; storeBusy = false }
        }
    }

    LaunchedEffect(balance, activeProPlan) { if (!storeBusy) loading = false }

    ModalBottomSheet(
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 0.dp,
        scrimColor = Color(0xFF0B1220).copy(alpha = .48f),
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.statusBarsPadding().padding(top = 12.dp),
        containerColor = MColors.Canvas,
        dragHandle = { MPremiumSheetHandle() }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                MSheetHeading(
                    title = "Cüzdan & Mağaza",
                    subtitle = "Token bakiyen ve üyelik ayrıcalıkların",
                    icon = Icons.Rounded.AccountBalanceWallet,
                    onDismiss = onDismiss
                )
            }

            if (remoteStorePending && catalogError != null) item { Text(catalogError.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MColors.Ink500) }
            // Segmented Geçiş: Token Paketleri / Matchora Plus
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 3.dp,
                    border = BorderStroke(1.dp, Color(0xFFE4E9F2)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        WalletSheetTab.entries.forEach { tab ->
                            val selected = activeTab == tab
                            val bg by animateColorAsState(
                                if (selected) MColors.Ink900 else Color.Transparent,
                                tween(200),
                                label = "wallet_tab_bg"
                            )
                            Surface(
                                onClick = { activeTab = tab },
                                shape = RoundedCornerShape(12.dp),
                                color = bg,
                                shadowElevation = if (selected) 4.dp else 0.dp,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 9.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (tab == WalletSheetTab.TOKEN_PACKS) Icons.Rounded.Toll else Icons.Rounded.WorkspacePremium,
                                        contentDescription = null,
                                        tint = if (selected) Color(0xFFFFB37B) else MColors.Ink500,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = tab.label,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) Color.White else MColors.Ink600
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (activeTab == WalletSheetTab.TOKEN_PACKS) {
                // ═══════════════════════════════════════════════════════════════
                // 1. TOKEN PAKETLERİ (CONSUMABLE IN-APP PURCHASE)
                // ═══════════════════════════════════════════════════════════════
                item {
                    MatchoraWalletCard(holder = holder, balance = balance, locked = locked)
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DemoData.tokenPacks.chunked(2).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { pack ->
                                    val isSelected = selectedPack.sku == pack.sku
                                    MCard(
                                        modifier = Modifier.weight(1f),
                                        borderColor = if (isSelected) MColors.BrandDeep else MColors.Line,
                                        onClick = { selectedPack = pack }
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(if (isSelected) Color(0xFFFFF9F5) else Color.White)
                                                .padding(horizontal = 10.dp, vertical = 14.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            if (pack.tag != null) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFFFFB37B).copy(alpha = 0.25f)
                                                ) {
                                                    Text(
                                                        text = pack.tag,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MColors.BrandDeep,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            } else {
                                                Spacer(Modifier.height(14.dp))
                                            }

                                            Text(
                                                text = "${pack.tokens} TKN",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MColors.Ink900
                                            )
                                            Text(
                                                text = if (pack.bonus > 0) "+${pack.bonus} bonus" else "Standart",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (pack.bonus > 0) Color(0xFF16A34A) else MColors.Ink500,
                                                fontWeight = if (pack.bonus > 0) FontWeight.Bold else FontWeight.Normal
                                            )
                                            Text(
                                                text = price(pack.sku, pack.priceLabel),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MColors.BrandDeep
                                            )
                                        }
                                    }
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }

                item {
                    // Apple Pay / Google StoreKit Satın Alma Butonu
                    Surface(
                        onClick = {
                            if (remoteStorePending) storeAction(selectedPack.sku)
                            else if (!loading) {
                                loading = true
                                onLoad(selectedPack)
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black,
                        shadowElevation = 5.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .shadow(6.dp, RoundedCornerShape(16.dp), ambientColor = Color(0x30000000), spotColor = Color(0x50000000))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Rounded.ShoppingBag, null, Modifier.size(20.dp), tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (loading) "İşleniyor…" else "${selectedPack.tokens + selectedPack.bonus} TKN al · ${price(selectedPack.sku, selectedPack.priceLabel)}",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                val purchases = ledger.filter { when (historyFilter) { 1 -> it.type in setOf(LedgerType.PURCHASE, LedgerType.BONUS); 2 -> it.type in setOf(LedgerType.HOLD, LedgerType.RELEASE); else -> true } }
                item {
                    Surface(
                        onClick = { showHistorySection = !showHistorySection },
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shadowElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Rounded.ReceiptLong, null, tint = MColors.BrandDeep, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "Cüzdan hareketleri",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MColors.Ink900
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (ledger.isEmpty()) "Henüz hareket yok" else "${ledger.size} işlem",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MColors.Ink500
                                )
                                Icon(
                                    imageVector = if (showHistorySection) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MColors.Ink500,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
                if (showHistorySection) {
                    if (ledger.isNotEmpty()) {
                        item { MSegmentedControl(listOf("Tümü", "Yükleme", "Kilitler"), historyFilter, { historyFilter = it }) }
                        if (purchases.isEmpty()) {
                            item {
                                MCard(Modifier.fillMaxWidth()) {
                                    Text("Bu kategoride henüz bir hareket yok.", Modifier.padding(18.dp), color = MColors.Ink500)
                                }
                            }
                        } else {
                            items(purchases, key = { it.id }) { entry ->
                                MCard(Modifier.fillMaxWidth()) {
                                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Surface(shape = CircleShape, color = entry.type.tint, modifier = Modifier.size(36.dp)) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(if (entry.amount < 0) Icons.Rounded.Remove else Icons.Rounded.Add, null, tint = entry.type.color, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(entry.description, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MColors.Ink900)
                                            Text("${entry.type.label} · ${entry.time}", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
                                        }
                                        Text("${if (entry.amount > 0) "+" else ""}${entry.amount} TKN", color = entry.type.color, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleSmall)
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            MCard(Modifier.fillMaxWidth()) {
                                Text("Henüz cüzdan hareketin bulunmuyor.", Modifier.padding(16.dp), color = MColors.Ink500)
                            }
                        }
                    }
                    item { PersonalHistoryButton("wallet", "Tüm cüzdan geçmişi") }
                }
            } else {
                // ═══════════════════════════════════════════════════════════════
                // 2. MATCHORA PLUS ABONELİĞİ (VIP KART & PLAN SEÇİMİ)
                // ═══════════════════════════════════════════════════════════════
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x300B1220), spotColor = Color(0x450B1220)),
                        shape = RoundedCornerShape(24.dp),
                        color = MColors.Ink900,
                        border = BorderStroke(1.2.dp, Color(0xFFE1C49D).copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Brush.linearGradient(listOf(Color(0xFF233740), MColors.Ink900)))
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .background(Brush.linearGradient(listOf(Color(0xFFF3D5A5), Color(0xFFC98B4B))), RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Rounded.WorkspacePremium, null, tint = MColors.Ink900, modifier = Modifier.size(22.dp))
                                    }
                                    Column {
                                        Text("MATCHORA PLUS", color = Color(0xFFE1C49D), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)
                                        Text("VIP SPORCU ÜYELİĞİ", color = Color(0xFF9EAFBB), fontSize = 9.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(9.dp),
                                    color = if (activeProPlan != null) Color(0xFF16A34A) else Color(0xFF344852),
                                    border = BorderStroke(0.8.dp, Color(0xFFE1C49D).copy(alpha = 0.35f))
                                ) {
                                    Text(
                                        text = if (activeProPlan != null) "AKTİF ÜYE" else "+100 TKN / AY",
                                        color = if (activeProPlan != null) Color.White else Color(0xFFF3D5A5),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                            val plusFeatures = listOf(
                                Triple(Icons.Rounded.Toll, "Her ay 100 TKN", "Her dönem cüzdanına otomatik yüklenir"),
                                Triple(Icons.Rounded.WorkspacePremium, "Altın Pasaport Çerçevesi", "Profilinde ve kadrolarda VIP sporcu görünümü"),
                                Triple(Icons.Rounded.Bolt, "Öncelikli Eşleşme & İlan", "Aramalarda ve spor radarında öne çık")
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                plusFeatures.forEach { (featureIcon, title, detail) ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(9.dp),
                                            color = Color.White.copy(alpha = 0.08f),
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(featureIcon, null, tint = Color(0xFFF3D5A5), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(title, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text(detail, fontSize = 10.5.sp, color = Color(0xFFB8C7CE))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    val plans = DemoData.proPlans
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        plans.forEach { plan ->
                            val isSelected = plan.sku == selectedPlanSku
                            Surface(
                                onClick = { selectedPlanSku = plan.sku },
                                shape = RoundedCornerShape(18.dp),
                                color = if (isSelected) Color(0xFFFFF8F3) else Color.White,
                                border = BorderStroke(if (isSelected) 1.6.dp else 1.dp, if (isSelected) MColors.BrandDeep else Color(0xFFE2E8F0)),
                                shadowElevation = if (isSelected) 4.dp else 1.dp,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(14.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(plan.title, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = MColors.Ink900)
                                        Icon(
                                            imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                            contentDescription = null,
                                            tint = if (isSelected) MColors.BrandDeep else MColors.Ink300,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    val resolvedPrice = price(plan.sku, plan.priceLabel)
                                    val showPeriod = !remoteStorePending || catalog.any { it.sku == plan.sku }
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.Bottom
                                        ) {
                                            Text(
                                                text = resolvedPrice,
                                                fontSize = if (showPeriod) 15.5.sp else 12.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isSelected) MColors.BrandDeep else MColors.Ink900,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (showPeriod) {
                                                Text(
                                                    text = " ${plan.period}",
                                                    fontSize = 11.sp,
                                                    color = MColors.Ink500,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    modifier = Modifier.padding(bottom = 1.dp)
                                                )
                                            }
                                        }
                                        if (plan.note != null) {
                                            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFDCFCE7)) {
                                                Text(
                                                    text = plan.note,
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF15803D),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        } else {
                                            Text("İstediğin zaman iptal", fontSize = 10.sp, color = MColors.Ink400, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    val planToSubscribe = DemoData.proPlans.firstOrNull { it.sku == selectedPlanSku } ?: DemoData.proPlans.first()
                    val currentPlanActive = activeProPlan != null && activeProPlan.sku == planToSubscribe.sku
                    Surface(
                        onClick = {
                            if (currentPlanActive) return@Surface
                            if (remoteStorePending) storeAction(planToSubscribe.sku)
                            else if (!loading) {
                                loading = true
                                onSubscribePlan(planToSubscribe)
                            }
                        },
                        enabled = !currentPlanActive,
                        shape = RoundedCornerShape(16.dp),
                        color = if (currentPlanActive) Color(0xFF16A34A) else MColors.Ink900,
                        shadowElevation = 5.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (currentPlanActive) {
                                Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Matchora Plus Aktif", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            } else {
                                Icon(Icons.Rounded.WorkspacePremium, null, Modifier.size(20.dp), tint = Color(0xFFF3D5A5))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = when {
                                        loading -> "İşleniyor…"
                                        activeProPlan != null -> "Plana Geç · ${price(planToSubscribe.sku, planToSubscribe.priceLabel)}"
                                        else -> "Plus’a Katıl · ${price(planToSubscribe.sku, planToSubscribe.priceLabel)}"
                                    },
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Abonelik otomatik yenilenir; App Store / Google Play hesap ayarlarından istediğin an iptal edebilirsin.",
                        fontSize = 10.5.sp,
                        color = MColors.Ink400,
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
            item { AppLegalLinks() }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = { if (remoteStorePending) storeAction() else storeNotice = "Önizleme modunda gerçek mağaza işlemi yapılmaz." }, enabled = !storeBusy) { Text("Satın alımları geri yükle", fontSize = 11.sp) }
                    TextButton(onClick = { if (remoteStorePending) { runCatching { uriHandler.openUri(if (storePlatform == "ios") "https://apps.apple.com/account/subscriptions" else "https://play.google.com/store/account/subscriptions") }.onFailure { storeNotice = "Mağaza hesabı açılamadı." } } else storeNotice = if (activeProPlan == null) "Aktif bir Plus planı bulunmuyor." else "${activeProPlan.title} · ${activeProPlan.priceLabel} ${activeProPlan.period}. Gerçek abonelik değişiklikleri mağaza hesabından yönetilir." }) { Text("Üyeliği yönet", fontSize = 11.sp) }
                }
            }
        }
    }
    storeNotice?.let { notice -> AlertDialog(onDismissRequest = { storeNotice = null }, title = { Text("Mağaza hesabı") }, text = { Text(notice) }, confirmButton = { TextButton(onClick = { storeNotice = null }) { Text("Tamam") } }) }
}
