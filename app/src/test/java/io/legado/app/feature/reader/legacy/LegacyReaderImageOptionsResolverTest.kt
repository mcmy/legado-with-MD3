package io.legado.app.feature.reader.legacy

import io.legado.app.feature.reader.core.layout.ReaderImageLayoutMode
import io.legado.app.feature.reader.core.layout.ReaderTextAlignment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LegacyReaderImageOptionsResolverTest {
    @Test
    fun parsesTypedImageUrlOptions() {
        val options = LegacyReaderImageOptionsResolver.resolve(
            "https://example/image, {\"style\":\"right\",\"width\":\"37.5%\",\"click\":\"open(\\\"x\\\")\"}",
        )!!
        assertEquals(ReaderImageLayoutMode.STANDALONE, options.layoutMode)
        assertEquals(ReaderTextAlignment.END, options.horizontalAlignment)
        assertEquals(.375f, options.requestedWidthFraction!!, 0f)
        assertEquals("open(\"x\")", options.action)
    }

    @Test
    fun parsesAbsoluteWidthAndKnownModesCaseInsensitively() {
        val options = LegacyReaderImageOptionsResolver.resolve("x,{\"style\":\"full\",\"width\":\"123\"}")!!
        assertEquals(ReaderImageLayoutMode.FULL_WIDTH, options.layoutMode)
        assertEquals(123f, options.requestedWidthPx!!, 0f)
    }

    @Test fun malformedOrAbsentOptionsDoNotInventOverrides() {
        assertNull(LegacyReaderImageOptionsResolver.resolve("x"))
        assertNull(LegacyReaderImageOptionsResolver.resolve("x,{bad}"))
    }
}
