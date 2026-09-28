package io.legado.app.feature.reader

import io.legado.app.feature.reader.core.model.ReaderBookmarkBadge
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderBookmarkBadgeDrawPolicyTest {
    private val custom = ReaderBookmarkBadge(
        leftPx = 0f,
        topPx = 0f,
        widthPx = 24,
        heightPx = 48,
        imageSource = "bookmark.png",
        imageVersion = "1",
    )

    @Test
    fun customBadgeWaitsForItsOwnLoadResult() {
        assertFalse(shouldDrawReaderBookmarkBadge(custom, null))
        assertFalse(shouldDrawReaderBookmarkBadge(custom, custom.copy(imageVersion = "2") to null))
        assertTrue(shouldDrawReaderBookmarkBadge(custom, custom to null))
    }

    @Test
    fun defaultBadgeDoesNotNeedAnAsyncImageResult() {
        assertTrue(shouldDrawReaderBookmarkBadge(custom.copy(imageSource = ""), null))
    }
}
