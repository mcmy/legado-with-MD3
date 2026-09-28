package io.legado.app.feature.reader.platform

import android.app.Application
import io.legado.app.feature.reader.core.layout.*
import io.legado.app.feature.reader.core.model.ReaderTextStyle
import io.legado.app.feature.reader.core.source.ReaderChapterSourceParser
import io.legado.app.feature.reader.core.source.ReaderTitleSegmentation
import io.legado.app.utils.textHeight
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReaderSubtitleMetricsTest {
    @Test fun subtitleUsesItsActualFontBoundsEvenWhenScaleIsOne() = runBlocking {
        val titleStyle = ReaderTextStyle(0, 32f)
        val titlePaint = ReaderAndroidPaintFactory.createTextPaint(titleStyle)
        val bodyStyle = ReaderTextStyle(0, 20f)
        val bodyPaint = ReaderAndroidPaintFactory.createTextPaint(bodyStyle)
        for (scale in listOf(1f, 0.5f, 1.3f)) {
            val source = ReaderChapterSourceParser.parse(0, "甲乙", listOf("正文"), true, false)
                .withTitleVisibility(true, ReaderTitleSegmentation(type = 1, distance = 1, subtitleScale = scale))
            val result = ReaderChapterBlockMeasurer(
                AndroidReaderTextShaper(bodyPaint), AndroidReaderTextShaper(titlePaint), { null },
                ReaderTextShaperFactory { AndroidReaderTextShaper(ReaderAndroidPaintFactory.createTextPaint(it)) },
            ).measure(source, ReaderChapterMeasureStyle(
                bodyStyle, titleStyle, 0, ReaderTextAlignment.START, ReaderTextAlignment.CENTER,
                bodyLineHeightPx = bodyPaint.textHeight,
                bodyBaselineOffsetPx = ReaderAndroidPaintFactory.baselineOffset(bodyPaint),
                titleLineHeightPx = titlePaint.textHeight,
                titleBaselineOffsetPx = ReaderAndroidPaintFactory.baselineOffset(titlePaint),
                titleLineSpacingMultiplier = 1.5f,
            )) as ReaderChapterMeasureResult.Success
            val paragraphs = result.blocks.filterIsInstance<ReaderMeasuredBlock.InlineParagraph>()
            val subtitlePaint = ReaderAndroidPaintFactory.createTextPaint(titleStyle.copy(fontSizePx = 32f * scale))
            val metrics = subtitlePaint.fontMetrics
            val subtitleHeight = metrics.bottom - metrics.top
            assertEquals("subtitle height at $scale", subtitleHeight, paragraphs[1].lineHeightPx, 0.001f)
            assertEquals(subtitleHeight - metrics.descent, paragraphs[1].baselineOffsetPx, 0.001f)
            assertEquals(subtitleHeight / titlePaint.textHeight, paragraphs[1].titleSpacingScale, 0.001f)
            assertEquals(titlePaint.textHeight, paragraphs[0].lineHeightPx, 0.001f)
            assertEquals(bodyPaint.textHeight, paragraphs[2].lineHeightPx, 0.001f)
            assertEquals(1.5f, paragraphs[1].lineSpacingMultiplier, 0f)
            assertEquals(listOf(0, 1), paragraphs[2].items.map { it.chapterPosition })
            val page = ReaderPaginator.paginateBlocks(result.blocks, ReaderPaginationConfig(
                0, "甲乙", 1000, 1000, 0f, 0f, 0f, 0f, bodyPaint.textHeight,
                ReaderAndroidPaintFactory.baselineOffset(bodyPaint),
                titleTopSpacingPx = 7f, titleBottomSpacingPx = 5f,
                titleParagraphSpacingPx = titlePaint.textHeight * 0.3f,
                titleSegmentSpacingPx = titlePaint.textHeight * 0.4f,
            )).single()
            val elements = page.elements.filterIsInstance<io.legado.app.feature.reader.core.model.ReaderElement.Text>()
            val subtitleTop = 7f + titlePaint.textHeight * (1.5f + 0.3f + 0.4f)
            assertEquals(subtitleTop, elements[1].bounds.top, 0.001f)
            assertEquals(subtitleTop + subtitleHeight - metrics.descent, elements[1].baselinePx, 0.001f)
            assertEquals(subtitleTop + subtitleHeight * (1.5f + 0.3f) + 5f, elements[2].bounds.top, 0.001f)
        }
    }
}
