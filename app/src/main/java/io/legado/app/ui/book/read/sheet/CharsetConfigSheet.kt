package io.legado.app.ui.book.read.sheet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.legado.app.R
import io.legado.app.constant.AppConst
import io.legado.app.model.ReadBook
import io.legado.app.ui.widget.components.AppTextField
import io.legado.app.ui.widget.components.alert.AppAlertDialog
import io.legado.app.ui.widget.components.button.series.SmallPlainButton
import io.legado.app.ui.widget.components.menuItem.RoundDropdownMenu
import io.legado.app.ui.widget.components.menuItem.RoundDropdownMenuItem

@Composable
fun CharsetConfigSheet(
    show: Boolean,
    onDismissRequest: () -> Unit,
) {
    var charset by remember { mutableStateOf(ReadBook.book?.charset ?: "UTF-8") }
    val charsetEntries = remember { AppConst.charsets }
    var expanded by remember { mutableStateOf(false) }
    val title = stringResource(R.string.set_charset)

    AppAlertDialog(
        show = show,
        onDismissRequest = onDismissRequest,
        title = title,
        content = {
            Box {
                AppTextField(
                    value = charset,
                    onValueChange = { charset = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = title,
                    singleLine = true,
                    trailingIcon = {
                        SmallPlainButton(
                            onClick = { expanded = !expanded },
                            selected = expanded,
                            icon = Icons.Default.KeyboardArrowDown,
                            contentDescription = title,
                        )
                    },
                )
                RoundDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    charsetEntries.forEach { entry ->
                        RoundDropdownMenuItem(
                            text = entry,
                            isSelected = charset == entry,
                            onClick = {
                                charset = entry
                                expanded = false
                            },
                        )
                    }
                }
            }
        },
        confirmText = stringResource(R.string.ok),
        onConfirm = {
            ReadBook.setCharset(charset)
            onDismissRequest()
        },
        dismissText = stringResource(R.string.cancel),
        onDismiss = onDismissRequest,
    )
}
