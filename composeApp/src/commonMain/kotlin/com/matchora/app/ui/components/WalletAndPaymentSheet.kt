package com.matchora.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.matchora.app.state.*
import com.matchora.app.ui.theme.MColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletAndPaymentSheet(onDismiss: () -> Unit, tokenBalance: Int, lockedTokens: Int,
    ledger: List<LedgerEntry>, paymentHolderName: String, onSelectTokenPack: (TokenPack) -> Unit) {
    TokenWalletSheet(onDismiss, tokenBalance, lockedTokens, paymentHolderName, ledger, onSelectTokenPack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TokenWalletSheet(onDismiss: () -> Unit, balance: Int, locked: Int, holder: String,
    ledger: List<LedgerEntry>, onLoad: (TokenPack) -> Unit,
    initialPack: TokenPack = DemoData.tokenPacks.getOrNull(1) ?: DemoData.tokenPacks.first()) {
    var selected by remember { mutableStateOf(initialPack) }
    var loading by remember { mutableStateOf(false) }
    LaunchedEffect(balance) { loading = false }
    ModalBottomSheet(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 0.dp,
        scrimColor = androidx.compose.ui.graphics.Color(0xFF0B1220).copy(alpha = .48f),
onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.statusBarsPadding().padding(top = 12.dp),
        containerColor = MColors.Canvas, dragHandle = { MPremiumSheetHandle() }) {
        LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { MSheetHeading("Token cüzdanı", "Bakiyeni yükle, oyuna katıl", Icons.Rounded.AccountBalanceWallet, onDismiss) }
            item { MatchoraWalletCard(holder, balance, locked) }
            item { MSectionHeader("Token yükle") }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DemoData.tokenPacks.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { pack ->
                        MCard(Modifier.weight(1f), borderColor = if (selected.sku == pack.sku) MColors.Brand else MColors.Line,
                            onClick = { selected = pack }) {
                            Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(if (selected.sku == pack.sku) Icons.Rounded.CheckCircle else Icons.Rounded.Toll, null, tint = MColors.BrandDeep)
                                Text("${pack.tokens} TKN", style = MaterialTheme.typography.titleSmall, color = MColors.Ink900)
                                Text(if (pack.bonus > 0) "+${pack.bonus} bonus" else "Standart", style = MaterialTheme.typography.labelSmall, color = MColors.Ink500)
                                Text(pack.priceLabel, style = MaterialTheme.typography.labelMedium, color = MColors.BrandDeep)
                            }
                        }
                    }
                }
            }
            }
            }
            item {
                MButton(if (loading) "Yükleniyor" else "${selected.tokens + selected.bonus} TKN yükle", {
                    if (!loading) { loading = true; onLoad(selected) }
                }, enabled = !loading, icon = Icons.Rounded.AddCircle)
            }
            item { MSectionHeader("Yükleme geçmişi") }
            val purchases = ledger.filter { it.type == LedgerType.PURCHASE }
            if (purchases.isEmpty()) item { Text("Henüz yükleme yapmadın.", color = MColors.Ink500) }
            items(purchases, key = { it.id }) { entry ->
                MCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AddCircleOutline, null, tint = MColors.BrandDeep)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Token yüklemesi", style = MaterialTheme.typography.titleSmall, color = MColors.Ink900)
                            Text(entry.time, style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
                        }
                        Text("+${entry.amount} TKN", color = MColors.BrandDeep, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}
