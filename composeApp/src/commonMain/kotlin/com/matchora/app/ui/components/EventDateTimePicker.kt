package com.matchora.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchora.app.state.*
import com.matchora.app.ui.theme.MColors
import kotlinx.datetime.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDateTimeField(date: LocalDate, hour: Int, minute: Int, onChange: (LocalDate, Int, Int) -> Unit) {
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    val timeLabel = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            onClick = { showDate = true },
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            shadowElevation = 1.dp,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.weight(1.15f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFF3EC),
                    border = BorderStroke(1.dp, MColors.Brand.copy(alpha = 0.2f)),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.CalendarMonth, null, tint = MColors.BrandDeep, modifier = Modifier.size(15.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Tarih", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = MColors.Ink500)
                    Text(
                        text = eventDateLabel(date),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MColors.Ink900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Surface(
            onClick = { showTime = true },
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            shadowElevation = 1.dp,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.weight(0.85f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFF3EC),
                    border = BorderStroke(1.dp, MColors.Brand.copy(alpha = 0.2f)),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Schedule, null, tint = MColors.BrandDeep, modifier = Modifier.size(15.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Saat", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = MColors.Ink500)
                    Text(
                        text = timeLabel,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MColors.BrandDeep,
                        maxLines = 1
                    )
                }
            }
        }
    }
    if (showDate) {
        val today = todayDate()
        val state = rememberDatePickerState(initialSelectedDateMillis = pickerMillis(date),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= pickerMillis(today)
                override fun isSelectableYear(year: Int) = year >= today.year
            })
        DatePickerDialog(onDismissRequest = { showDate = false }, confirmButton = {
            TextButton(onClick = { state.selectedDateMillis?.let { onChange(pickerDate(it), hour, minute) }; showDate = false }, enabled = state.selectedDateMillis != null) { Text("Tarihi seç") }
        }, dismissButton = { TextButton(onClick = { showDate = false }) { Text("Vazgeç") } }) {
            DatePicker(state, title = { Text("Buluşma tarihi", Modifier.padding(start = 24.dp, top = 16.dp)) }, showModeToggle = true)
        }
    }
    if (showTime) {
        val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
        var keyboard by remember { mutableStateOf(false) }
        AlertDialog(
            shape = RoundedCornerShape(24.dp),
            containerColor = Color(0xFFFAFBFC),
            tonalElevation = 0.dp,
            onDismissRequest = { showTime = false },
            title = { Text("Başlangıç saati") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (keyboard) TimeInput(state) else TimePicker(state)
                    MPillActionButton(
                        text = if (keyboard) "Saat kadranını aç" else "Klavyeyle gir",
                        icon = if (keyboard) Icons.Rounded.Schedule else Icons.Rounded.Keyboard,
                        tone = MPillTone.NEUTRAL,
                        compact = true,
                        onClick = { keyboard = !keyboard }
                    )
                }
            },
            confirmButton = { TextButton(onClick = { onChange(date, state.hour, state.minute); showTime = false }) { Text("Saati seç") } },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("Vazgeç") } }
        )
    }
}
