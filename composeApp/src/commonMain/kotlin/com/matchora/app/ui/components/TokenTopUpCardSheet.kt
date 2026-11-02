package com.matchora.app.ui.components

import androidx.compose.runtime.Composable
import com.matchora.app.state.*

@Composable
fun TokenTopUpCardSheet(onDismiss: () -> Unit, onConfirmTopUp: (TokenPack, String) -> Unit,
    initialPack: TokenPack = DemoData.tokenPacks.getOrNull(1) ?: DemoData.tokenPacks.first(),
    currentBalance: Int = 120, userName: String = "Matchora Sporcusu", ledger: List<LedgerEntry> = emptyList(),
    lockedTokens: Int = 0, activeProPlan: ProPlan? = null, onSubscribePlan: (ProPlan) -> Unit = {}) {
    TokenWalletSheet(onDismiss = onDismiss, balance = currentBalance, locked = lockedTokens, holder = userName, ledger = ledger,
        activeProPlan = activeProPlan, onSubscribePlan = onSubscribePlan,
        onLoad = { onConfirmTopUp(it, "Token paketi") }, initialPack = initialPack)
}
