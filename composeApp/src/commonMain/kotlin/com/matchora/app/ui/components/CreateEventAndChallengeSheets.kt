package com.matchora.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.matchora.app.ui.theme.MColors

// ═══════════════════════════════════════════════════════════════════════════════
// ŞİKAYET & İTİRAZ / DISPUTE DİYALOĞU (Strateji Dokümanı §12, §23)
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun ReportAndDisputeDialog(
    targetName: String,
    onDismiss: () -> Unit,
    onSubmitReport: (reason: String, note: String) -> Unit
) {
    var selectedReason by remember { mutableStateOf("No-show (Haber vermeden maça gelmedi)") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        tonalElevation = 0.dp,

        onDismissRequest = onDismiss,
        title = {
            Text("İtiraz / Bildirim: $targetName", fontWeight = FontWeight.Bold, color = MColors.Ink900)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Bu işlem moderasyon ve güven ekibi tarafından incelenir. Sahte bildirimler bildiren kullanıcının güven skorunu düşürür.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MColors.Ink600
                )

                val reasons = listOf(
                    "No-show (Haber vermeden maça gelmedi)",
                    "Son dakika iptali (Maçı riske attı)",
                    "Fair play / Sportmenlik dışı davranış",
                    "Yanlış skor beyanı (Rating itirazı)",
                    "Uygunsuz içerik / İletişim"
                )
                MFloatingDropdownBox(
                    label = "Bildirim Nedeni",
                    selectedValue = selectedReason,
                    options = reasons,
                    optionLabel = { it },
                    onSelect = { selectedReason = it },
                    icon = Icons.Rounded.ReportProblem
                )

                MFloatingInputBox(
                    label = "Ek Açıklama",
                    value = note,
                    onValueChange = { note = it },
                    placeholder = "Olayla ilgili kısa not...",
                    icon = Icons.Rounded.EditNote,
                    badgeText = "Opsiyonel",
                    singleLine = false,
                    minLines = 2,
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmitReport(selectedReason, note)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MColors.Rose)
            ) {
                Text("Bildirimi Gönder", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Vazgeç", color = MColors.Ink500)
            }
        },
        containerColor = MColors.Surface
    )
}
