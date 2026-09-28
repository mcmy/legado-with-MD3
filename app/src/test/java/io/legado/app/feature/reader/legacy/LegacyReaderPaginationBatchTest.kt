package io.legado.app.feature.reader.legacy

import io.legado.app.feature.reader.core.model.ReaderPage
import io.legado.app.feature.reader.core.model.ReaderPageId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking

class LegacyReaderPaginationBatchTest {
    @Test
    fun chapterLayoutIdentityChangesForPresentationAndResolutionInputs() {
        val identity = layoutIdentity()

        assertFalse(identity == layoutIdentity(displayTitle = "new title"))
        assertFalse(identity == layoutIdentity(isVolume = true))
        assertFalse(identity == layoutIdentity(sourceHash = 2))
        assertFalse(identity == layoutIdentity(chapterBaseUrl = "https://cdn.example/"))
        assertFalse(identity == layoutIdentity(bookSourceHash = 2))
        assertEquals(identity, identity.copy())
    }

    @Test
    fun chapterExceptionBecomesAnExplicitLocalFailure() = runBlocking {
        val result = paginateLegacyReaderChapterSafely {
            error("broken chapter")
        }

        assertEquals(
            LegacyReaderChapterPaginationResult.Unsupported("exception:IllegalStateException"),
            result,
        )
    }

    @Test
    fun cancellationStillStopsPaginationGeneration() {
        try {
            runBlocking {
                paginateLegacyReaderChapterSafely {
                    throw CancellationException("new generation")
                }
            }
            fail("CancellationException should propagate")
        } catch (_: CancellationException) {
            // Expected: a new pagination generation must stop this one immediately.
        }
    }

    @Test
    fun adjacentFailureKeepsCurrentPagesAndReportsTheFailure() {
        val batch = collectLegacyReaderPaginationBatch(
            currentChapterIndex = 2,
            results = listOf(
                1 to LegacyReaderChapterPaginationResult.Unsupported("html"),
                2 to LegacyReaderChapterPaginationResult.Success(listOf(page(2))),
                3 to LegacyReaderChapterPaginationResult.Success(listOf(page(3))),
            ),
        )

        assertTrue(batch.hasCurrentChapter)
        assertEquals(listOf(2, 3), batch.pages.map { it.id.chapterIndex })
        assertEquals(mapOf(1 to "html"), batch.unsupportedChapters)
    }

    @Test
    fun currentFailureIsExplicitEvenWhenAdjacentPagesSucceeded() {
        val batch = collectLegacyReaderPaginationBatch(
            currentChapterIndex = 2,
            results = listOf(
                1 to LegacyReaderChapterPaginationResult.Success(listOf(page(1))),
                2 to LegacyReaderChapterPaginationResult.Unsupported("image-size:x"),
            ),
        )

        assertFalse(batch.hasCurrentChapter)
        assertEquals(listOf(1), batch.pages.map { it.id.chapterIndex })
        assertEquals(mapOf(2 to "image-size:x"), batch.unsupportedChapters)
        assertEquals("image-size:x", batch.failureReasonFor(2))
        assertEquals(null, batch.failureReasonFor(1))
    }

    private fun page(chapterIndex: Int) = ReaderPage(
        id = ReaderPageId(chapterIndex, 0),
        chapterTitle = "chapter-$chapterIndex",
        text = "content",
        widthPx = 100,
        heightPx = 100,
        contentTopPx = 0f,
        contentBottomPx = 100f,
        elements = emptyList(),
        revision = 1,
    )

    private fun layoutIdentity(
        displayTitle: String = "chapter",
        isVolume: Boolean = false,
        sourceHash: Int = 1,
        chapterBaseUrl: String = "https://example/",
        bookSourceHash: Int = 1,
    ) = LegacyReaderChapterLayoutIdentity(
        chapterIndex = 1,
        chapterUrl = "chapter-url",
        chapterBaseUrl = chapterBaseUrl,
        displayTitle = displayTitle,
        isVolume = isVolume,
        contentHash = 1,
        contentProcessesHash = 1,
        sourceHash = sourceHash,
        bookUrl = "book-url",
        bookOrigin = "origin",
        bookSourceHash = bookSourceHash,
    )
}
