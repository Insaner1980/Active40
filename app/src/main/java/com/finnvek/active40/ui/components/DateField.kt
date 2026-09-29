package com.finnvek.active40.ui.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.finnvek.active40.R
import com.finnvek.active40.ui.formatDayLong
import com.finnvek.active40.ui.theme.LightSurfaceTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

// DatePicker käsittelee päiviä UTC-keskiyön millisekunteina.
private fun LocalDate.toPickerMillis() = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDate() = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

/** Päivämäärärivi, josta avautuu päivämäärävalitsin. Tulevia päiviä ei voi valita. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    val formatted = formatDayLong(date)
    val description = stringResource(R.string.change_date, formatted)
    GlassListRow(
        title = formatted,
        supportingText = label,
        onClick = { open = true },
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description },
    )
    if (open) {
        val today = LocalDate.now()
        val pickerState =
            rememberDatePickerState(
                initialSelectedDateMillis = date.toPickerMillis(),
                selectableDates =
                    object : SelectableDates {
                        override fun isSelectableDate(utcTimeMillis: Long) = !utcTimeMillis.toLocalDate().isAfter(today)

                        override fun isSelectableYear(year: Int) = year <= today.year
                    },
            )
        LightSurfaceTheme {
            DatePickerDialog(
                onDismissRequest = { open = false },
                confirmButton = {
                    TextButton(onClick = {
                        pickerState.selectedDateMillis?.let { onDateChange(it.toLocalDate()) }
                        open = false
                    }) { Text(stringResource(R.string.action_ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { open = false }) { Text(stringResource(R.string.action_cancel)) }
                },
            ) {
                DatePicker(state = pickerState)
            }
        }
    }
}
