package com.matchora.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.matchora.app.state.*
import kotlinx.datetime.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDateTimeField(date: LocalDate, hour: Int, minute: Int, onChange: (LocalDate, Int, Int) -> Unit) {
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MButton(eventDateLabel(date), { showDate = true }, Modifier.weight(1f), Icons.Rounded.CalendarMonth, isPrimary = false)
        MButton("${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}", { showTime = true }, Modifier.weight(.7f), Icons.Rounded.Schedule, isPrimary = false)
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
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        containerColor = androidx.compose.ui.graphics.Color(0xFFFAFBFC),
        tonalElevation = 0.dp,
onDismissRequest = { showTime = false }, title = { Text("Başlangıç saati") },
            text = {
                Column {
                    if (keyboard) TimeInput(state) else TimePicker(state)
                    TextButton(onClick = { keyboard = !keyboard }) { Text(if (keyboard) "Saat kadranını aç" else "Klavyeyle gir") }
                }
            }, confirmButton = { TextButton(onClick = { onChange(date, state.hour, state.minute); showTime = false }) { Text("Saati seç") } },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("Vazgeç") } })
    }
}
