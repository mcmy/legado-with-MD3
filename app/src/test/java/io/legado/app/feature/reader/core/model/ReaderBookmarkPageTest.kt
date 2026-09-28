package io.legado.app.feature.reader.core.model

import io.legado.app.data.entities.Bookmark
import io.legado.app.feature.reader.core.navigation.ReaderPageNavigator
import io.legado.app.model.ReaderBookmarkState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderBookmarkPageTest {
    @After fun clearSnapshot() = ReaderBookmarkState.clear()

    private val pages = (0..2).map { index ->
        ReaderPage(ReaderPageId(3, index), "章", "字", 100, 200, 20f, 180f,
            listOf(ReaderElement.Text(ReaderRect(10f, 20f, 20f, 30f), 28f, "字",
                ReaderTextStyle(0, 10f), false, false, chapterPosition = index * 10)), 1L)
    }

    private fun visibleBadges(bookName: String = "书"): List<Boolean> = pages.indices.map { index ->
        val context = ReaderPageNavigator.pageContext(pages, index)!!
        val bookmarked = ReaderBookmarkState.hasBookmarkInRange(bookName, "作者", context.chapterIndex,
            context.startPosition, context.endPosition)
        ReaderBookmarkBadge.create(bookmarked, false, 100, 20f, 10, 1f, 10) != null
    }

    @Test fun updatingSnapshotOnlyMarksTheOwningPageAndClearsAfterRemoval() {
        ReaderBookmarkState.clear()
        assertEquals(listOf(false, false, false), visibleBadges())
        ReaderBookmarkState.update("书", "作者", listOf(Bookmark(bookName = "书", bookAuthor = "作者",
            chapterIndex = 3, chapterPos = 10)))
        assertEquals(listOf(false, true, false), visibleBadges())
        assertEquals(listOf(false, false, false), visibleBadges("另一本书"))
        ReaderBookmarkState.update("书", "作者", emptyList())
        assertEquals(listOf(false, false, false), visibleBadges())
    }
}
