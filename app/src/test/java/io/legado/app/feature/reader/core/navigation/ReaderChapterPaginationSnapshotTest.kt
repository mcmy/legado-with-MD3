package io.legado.app.feature.reader.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderChapterPaginationSnapshotTest {
    private val snapshot = ReaderChapterPaginationSnapshot(
        chapterIndex = 4,
        pageStarts = listOf(0, 120, 260),
        contentEnd = 340,
        generation = 7,
    )

    @Test
    fun resolvesPageIndexAndAdjacentStarts() {
        assertEquals(0, snapshot.pageIndex(0))
        assertEquals(1, snapshot.pageIndex(200))
        assertEquals(2, snapshot.pageIndex(Int.MAX_VALUE))
        assertEquals(120, snapshot.nextPageStart(30))
        assertEquals(120, snapshot.previousPageStart(300))
    }

    @Test
    fun reportsChapterBoundaries() {
        assertNull(snapshot.previousPageStart(0))
        assertNull(snapshot.nextPageStart(300))
        assertEquals(260, snapshot.lastPageStart)
        assertEquals(3, snapshot.pageCount)
    }
}
