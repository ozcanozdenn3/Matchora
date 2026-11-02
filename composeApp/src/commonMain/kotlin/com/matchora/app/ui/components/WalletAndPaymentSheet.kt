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
    var storeNotice by remember { mutableStateOf<String?>(null) }
    var historyFilter by remember { mutableIntStateOf(0) }

    LaunchedEffect(balance, activeProPlan) { loading = false }

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
                    subtitle = "In-App Purchase ve abonelik merkezi",
                    icon = Icons.Rounded.AccountBalanceWallet,
                    onDismiss = onDismiss
                )
            }

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

                // Apple Pay & Google Play Mağaza Güvencesi Rozeti
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.VerifiedUser, null, tint = MColors.BrandDeep, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Apple In-App Purchase & Google Play Billing ile tek tıkla güvenli ödeme",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MColors.Ink700
                            )
                        }
                    }
                }

                item { MSectionHeader("Token Paketi Seç") }

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
                                                text = pack.priceLabel,
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
                            if (!loading) {
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
                                text = if (loading) "İşleniyor…" else "${selectedPack.tokens + selectedPack.bonus} TKN al · ${selectedPack.priceLabel}",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                item { MSectionHeader("Cüzdan hareketleri") }
                item { MSegmentedControl(listOf("Tümü", "Yükleme", "Kilitler"), historyFilter, { historyFilter = it }) }
                val purchases = ledger.filter { when (historyFilter) { 1 -> it.type in setOf(LedgerType.PURCHASE, LedgerType.BONUS); 2 -> it.type in setOf(LedgerType.HOLD, LedgerType.RELEASE); else -> true } }
                if (purchases.isEmpty()) {
                    item {
                        MCard(Modifier.fillMaxWidth()) {
                            Text("Bu kategoride henüz bir hareket yok.", Modifier.padding(18.dp), color = MColors.Ink500)
                        }
                    }
                }
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
            } else {
                // ═══════════════════════════════════════════════════════════════
                // 2. MATChORA PLUS ABONELİĞİ (AUTO-RENEWABLE SUBSCRIPTION)
                // ═══════════════════════════════════════════════════════════════
                item {
                    // Altın & Lacivert VIP Üyelik Kartı
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x300B1220), spotColor = Color(0x450B1220)),
                        shape = RoundedCornerShape(24.dp),
                        color = MColors.Ink900,
                        border = BorderStroke(1.2.dp, Color(0xFFFFB37B).copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Brush.linearGradient(listOf(Color(0xFF1F2937), Color(0xFF111827))))
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .background(Brush.linearGradient(listOf(Color(0xFFFFD188), Color(0xFFEA580C))), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Rounded.WorkspacePremium, null, tint = MColors.Ink900, modifier = Modifier.size(20.dp))
                                    }
                                    Text("MATChORA PLUS", color = Color(0xFFFFB37B), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                                }

                                if (activeProPlan != null) {
                                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF16A34A)) {
                                        Text("AKTİF ÜYE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                                    }
                                }
                            }

                            Text(
                                text = "Sınırsız maç deneyimi ve öncelikli sporcu ayrıcalıkları",
                                fontSize = 13.sp,
                                color = Color(0xFFE2E8F0),
                                lineHeight = 18.sp
                            )

                            HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

                            // Avantaj Maddeleri
                            val benefits = listOf(
                                "⚡ Her ay 100 TKN Hediye (₺100 değerinde)",
                                "👑 Altın Onaylı Sporcu Rozeti & vitrinde öne çıkma",
                                "🎟️ Sınırsız & Komisyonsuz Maç İlanı Açma",
                                "🔍 Akıllı Eşleşmede VIP Radar Önceliği"
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                benefits.forEach { benefit ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFFFFB37B), modifier = Modifier.size(16.dp))
                                        Text(benefit, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }

                item { MSectionHeader("Abonelik Planı") }

                item {
                    val plans = DemoData.proPlans
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        plans.forEach { plan ->
                            val isSelected = plan.sku == selectedPlanSku
                            Surface(
                                onClick = { selectedPlanSku = plan.sku },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) Color(0xFFFFF9F5) else Color.White,
                                border = BorderStroke(1.2.dp, if (isSelected) MColors.BrandDeep else Color(0xFFE2E8F0)),
                                shadowElevation = if (isSelected) 4.dp else 1.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(plan.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MColors.Ink900)
                                            if (plan.note != null) {
                                                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFDCFCE7)) {
                                                    Text(plan.note, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                }
                                            }
                                        }
                                        Text("${plan.priceLabel} ${plan.period}", fontSize = 13.sp, color = MColors.BrandDeep, fontWeight = FontWeight.SemiBold)
                                    }

                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedPlanSku = plan.sku },
                                        colors = RadioButtonDefaults.colors(selectedColor = MColors.BrandDeep)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    val planToSubscribe = DemoData.proPlans.firstOrNull { it.sku == selectedPlanSku } ?: DemoData.proPlans.first()
                    Surface(
                        onClick = {
                            if (!loading && activeProPlan == null) {
                                loading = true
                                onSubscribePlan(planToSubscribe)
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = if (activeProPlan != null) Color(0xFF16A34A) else Color.Black,
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
                            if (activeProPlan != null) {
                                Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Matchora Plus Aktif", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            } else {
                                Icon(Icons.Rounded.ShoppingBag, null, Modifier.size(20.dp), tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (loading) "İşleniyor…" else "Abone ol · ${planToSubscribe.priceLabel}",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Apple App Store & Google Play Zorunlu Abonelik Metni
                item {
                    Text(
                        text = "Abonelik Apple ID / Google Play hesabınızdan tahsil edilir ve dönem bitiminde otomatik olarak yenilenir. İstediğiniz zaman App Store / Google Play abonelik ayarlarınızdan iptal edebilirsiniz.",
                        fontSize = 10.5.sp,
                        color = MColors.Ink400,
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = { storeNotice = "Geri yükleme arayüzü hazır. Mağaza bağlantısı açıldığında satın almaların hesabınla eşleştirilecek." }) { Text("Satın alımları geri yükle", fontSize = 11.sp) }
                    TextButton(onClick = { storeNotice = if (activeProPlan == null) "Aktif bir Plus planı bulunmuyor." else "${activeProPlan.title} · ${activeProPlan.priceLabel} ${activeProPlan.period}. Gerçek abonelik değişiklikleri mağaza hesabından yönetilir." }) { Text("Üyeliği yönet", fontSize = 11.sp) }
                }
            }
        }
    }
    storeNotice?.let { notice -> AlertDialog(onDismissRequest = { storeNotice = null }, title = { Text("Mağaza hesabı") }, text = { Text(notice) }, confirmButton = { TextButton(onClick = { storeNotice = null }) { Text("Tamam") } }) }
}
