package io.legado.app.feature.reader.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.legado.app.R
import io.legado.app.domain.model.settings.ReadStyleItem
import io.legado.app.ui.widget.components.alert.AppAlertDialog
import io.legado.app.ui.widget.components.settingItem.TinySwitchSettingItem
import io.legado.app.ui.widget.components.text.AppText
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun QuickReadStyleDialog(
    items: ImmutableList<ReadStyleItem>,
    onCheckedChange: (Int, Boolean) -> Unit,
    onDismissRequest: () -> Unit,
) {
    AppAlertDialog(
        show = true,
        title = stringResource(R.string.quick_read_style),
        onDismissRequest = onDismissRequest,
        content = {
            AppText(text = stringResource(R.string.quick_read_style_selection_hint))
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                    TinySwitchSettingItem(
                        title = item.name.ifBlank { "${stringResource(R.string.theme)} ${index + 1}" },
                        checked = item.quickStyleSelect,
                        onCheckedChange = { onCheckedChange(index, it) },
                    )
                }
            }
        },
        confirmText = stringResource(R.string.close),
        onConfirm = onDismissRequest,
    )
}
