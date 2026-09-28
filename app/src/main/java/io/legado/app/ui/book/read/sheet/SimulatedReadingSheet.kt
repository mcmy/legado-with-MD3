package io.legado.app.ui.book.read.sheet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.legado.app.R
import io.legado.app.model.ReadBook
import io.legado.app.ui.widget.components.AppTextField
import io.legado.app.ui.widget.components.alert.AppAlertDialog
import io.legado.app.ui.widget.components.settingItem.TinySwitchSettingItem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulatedReadingSheet(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onApply: () -> Unit,
) {
    val book = ReadBook.book ?: return
    var enabled by remember { mutableStateOf(book.getReadSimulating()) }
    var startChapter by remember { mutableStateOf(book.getStartChapter().toString()) }
    var dailyChapters by remember { mutableStateOf(book.getDailyChapters().toString()) }
    var startDate by remember { mutableStateOf(book.getStartDate() ?: LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }

    AppAlertDialog(
        show = show,
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.simulated_reading),
        content = {
            Column {
                AppTextField(
                    value = startDate.format(dateFormatter),
                    onValueChange = {},
                    label = stringResource(R.string.start_from),
                    readOnly = true,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                )
                Spacer(modifier = Modifier.height(8.dp))
                TinySwitchSettingItem(
                    title = stringResource(R.string.simulated_reading),
                    checked = enabled,
                    onCheckedChange = { enabled = it },
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    AppTextField(
                        value = startChapter,
                        onValueChange = { startChapter = it },
                        label = stringResource(R.string.start_chapter),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    AppTextField(
                        value = dailyChapters,
                        onValueChange = { dailyChapters = it },
                        label = stringResource(R.string.daily_chapters),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        confirmText = stringResource(R.string.ok),
        onConfirm = {
            book.setStartDate(startDate)
            book.setDailyChapters(dailyChapters.toIntOrNull() ?: 0)
            book.setStartChapter(startChapter.toIntOrNull() ?: 0)
            book.setReadSimulating(enabled)
            book.save()
            onApply()
            onDismissRequest()
        },
        dismissText = stringResource(R.string.cancel),
        onDismiss = onDismissRequest,
    )

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
            .toEpochMilli(),
    )
    AppAlertDialog(
        show = showDatePicker,
        onDismissRequest = { showDatePicker = false },
        content = { DatePicker(state = datePickerState) },
        confirmText = stringResource(R.string.ok),
        onConfirm = {
            datePickerState.selectedDateMillis?.let { millis ->
                startDate = Instant.ofEpochMilli(millis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            }
            showDatePicker = false
        },
        dismissText = stringResource(R.string.cancel),
        onDismiss = { showDatePicker = false },
    )
}
