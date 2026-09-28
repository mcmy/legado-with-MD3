package io.legado.app.feature.reader

import io.legado.app.feature.reader.core.model.ReaderPageTip
import io.legado.app.feature.reader.core.model.ReaderTipAlignment
import io.legado.app.feature.reader.core.model.ReaderTipRow
import io.legado.app.feature.reader.core.model.ReaderTipVisual
import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderTipResourceTest {

    @Test
    fun `dynamic tip missing from remembered map is created instead of throwing`() {
        val oldRow = tipRow("02:49")
        val updatedRow = tipRow("02:50")

        val resolved = resolveReaderTipResource(updatedRow, mapOf(oldRow to "old paint")) {
            "paint for ${it.tips.single().text}"
        }

        assertEquals("paint for 02:50", resolved)
    }

    private fun tipRow(text: String) = ReaderTipRow(
        visible = true,
        tips = listOf(
            ReaderPageTip(
                text = text,
                alignment = ReaderTipAlignment.END,
                visual = ReaderTipVisual.TEXT,
                batteryPercent = 26,
            ),
        ),
        colorArgb = 0,
        fontSizePx = 42f,
        fontPath = "",
        paddingLeftPx = 66f,
        paddingTopPx = 35f,
        paddingRightPx = 56f,
        paddingBottomPx = 0f,
        dividerColorArgb = null,
    )
}
