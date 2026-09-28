package io.legado.app.feature.reader.core.source

import io.legado.app.ui.book.read.page.provider.TitleStyleParser
import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderTitleSegmentationTest {
    @Test
    fun supportedOptionsMatchViewTitleParser() {
        val title = "第一章： 初遇。 再会"
        val options = listOf(
            ReaderTitleSegmentation(),
            ReaderTitleSegmentation(type = 1, distance = 4, subtitleScale = 0.7f),
            ReaderTitleSegmentation(type = 1, distance = 0),
            ReaderTitleSegmentation(type = 1, distance = 100),
            ReaderTitleSegmentation(type = 2, delimiter = "：,。", subtitleScale = 1.5f),
            ReaderTitleSegmentation(type = 2, delimiter = "["),
            ReaderTitleSegmentation(type = 3, delimiter = "[：。]", subtitleScale = 0.5f),
            ReaderTitleSegmentation(type = 3, delimiter = ""),
        )
        options.forEach { config ->
            val expected = TitleStyleParser.getSegments(
                title, config.type, config.distance, config.delimiter, config.subtitleScale,
            )
            val actual = config.blocks(title)
            assertEquals(config.toString(), expected.map { it.text }, actual.map { it.value })
            assertEquals(config.toString(), expected.map { it.scale }, actual.map { it.fontSizeScale })
            assertEquals(config.toString(), expected.map { !it.isMainTitle }, actual.map { it.isSubtitle })
        }
    }

    @Test
    fun eachOriginalTitleLineStartsWithMainTitleAndOffsetsFollowDisplayedText() {
        val blocks = ReaderTitleSegmentation(type = 1, distance = 2, subtitleScale = 0.5f)
            .blocks(" 甲乙丙丁 \n \n 戊己庚辛 ")
        assertEquals(listOf("甲乙", "丙丁", "戊己", "庚辛"), blocks.map { it.value })
        assertEquals(listOf(1f, 0.5f, 1f, 0.5f), blocks.map { it.fontSizeScale })
        assertEquals(listOf(false, true, false, true), blocks.map { it.isSubtitle })
        assertEquals(listOf(0, 3, 6, 9), blocks.map { it.chapterPosition })
    }

    @Test
    fun invalidOrEmptyDelimiterRetainsReadableTitle() {
        val title = "章名"
        assertEquals(listOf(title), ReaderTitleSegmentation(type = 3, delimiter = "[").blocks(title).map { it.value })
        assertEquals(listOf(title), ReaderTitleSegmentation(type = 2, delimiter = " , ").blocks(title).map { it.value })
    }

    @Test
    fun resegmentingCachedSourceKeepsBodyAndDoesNotAccumulateTitles() {
        val source = ReaderChapterSourceParser.parse(1, "第一章： 初遇", listOf("正文"), true, false)
        val segmented = source.withTitleVisibility(true, ReaderTitleSegmentation(type = 2, delimiter = "："))
        assertEquals("第一章：\n初遇\n", segmented.semanticTitle)
        assertEquals(source.semanticContent, segmented.semanticContent)
        assertEquals(source.characterCount, segmented.characterCount)
        assertEquals(source.blocks.last(), segmented.blocks.last())
        assertEquals(source, segmented.withTitleVisibility(true))
        assertEquals("", segmented.withTitleVisibility(false).semanticTitle)
    }
}
