package com.matchora.app.ui.components

import androidx.compose.runtime.Composable
import com.matchora.app.state.*

@Composable
fun TokenTopUpCardSheet(onDismiss: () -> Unit, onConfirmTopUp: (TokenPack, String) -> Unit,
    initialPack: TokenPack = DemoData.tokenPacks.getOrNull(1) ?: DemoData.tokenPacks.first(),
    currentBalance: Int = 120, userName: String = "Matchora Sporcusu", ledger: List<LedgerEntry> = emptyList()) {
    TokenWalletSheet(onDismiss, currentBalance, 0, userName, ledger,
        onLoad = { onConfirmTopUp(it, "Token yükleme") }, initialPack = initialPack)
}
