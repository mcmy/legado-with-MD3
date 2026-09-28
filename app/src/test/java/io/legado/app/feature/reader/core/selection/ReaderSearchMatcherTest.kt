package io.legado.app.feature.reader.core.selection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderSearchMatcherTest {
    private fun request(
        directIndex: Int = -1,
        occurrence: Int = 0,
        directLength: Int = 0,
        isRegex: Boolean = false,
    ) = ReaderSearchRequest(
        directIndex,
        directLength,
        occurrence,
        isRegex,
    )

    @Test
    fun `直达下标命中时直接采用`() {
        val content = "海边有座灯塔，灯塔很亮。"
        assertEquals(
            ReaderSearchMatch(7, 2),
            ReaderSearchMatcher.find(content, "灯塔", request(directIndex = 7, directLength = 2)),
        )
    }

    @Test
    fun `直达下标失效时退回按出现次数扫描`() {
        val content = "海边有座灯塔，灯塔很亮。"
        assertEquals(
            ReaderSearchMatch(7, 2),
            ReaderSearchMatcher.find(content, "灯塔", request(directIndex = 99, occurrence = 1)),
        )
    }

    @Test
    fun `直达下标指向其他文字时回退扫描`() {
        val content = "海边有座灯塔，灯塔很亮。"
        assertEquals(
            ReaderSearchMatch(4, 2),
            ReaderSearchMatcher.find(content, "灯塔", request(directIndex = 0, directLength = 2)),
        )
    }

    @Test
    fun `标题前缀后的直达下标映射到正文坐标`() {
        // 检索层的章节文本是“标题\\n正文”，Canvas 只以正文建立坐标。
        // 若不先扣标题长度，原下标可能碰巧指向正文中的另一个同词。
        val content = "灯塔在前，灯塔在后。"
        assertEquals(
            ReaderSearchMatch(0, 2),
            ReaderSearchMatcher.find(
                content,
                "灯塔",
                request(
                    directIndex = 3,
                    occurrence = 1,
                ),
                title = "标题",
            ),
        )
    }

    @Test
    fun `按出现次数回退时保留检索标题中的匹配`() {
        val content = "灯塔在前，灯塔在后。"
        assertEquals(
            ReaderSearchMatch(5, 2),
            ReaderSearchMatcher.find(
                content = content,
                query = "灯塔",
                request = request(directIndex = 99, occurrence = 2),
                title = "灯塔章节",
            ),
        )
    }

    @Test
    fun `标题命中使用标题坐标而不误标正文`() {
        assertEquals(
            ReaderSearchMatch(0, 2, isTitle = true),
            ReaderSearchMatcher.find(
                content = "正文也有灯塔。",
                query = "灯塔",
                request = request(directIndex = 0, directLength = 2),
                title = "灯塔章节",
            ),
        )
    }

    @Test
    fun `空查询或正文无结果返回 null`() {
        assertNull(ReaderSearchMatcher.find("海边有座灯塔。", "", request(directIndex = 0)))
        assertNull(ReaderSearchMatcher.find("海边有座灯塔。", "潜水艇", request()))
    }

    @Test
    fun `正则回退返回实际匹配长度`() {
        assertEquals(
            ReaderSearchMatch(7, 4),
            ReaderSearchMatcher.find(
                "第1章 起点\n第22章 终点",
                """第\d+章""",
                request(occurrence = 1, isRegex = true),
            ),
        )
    }

    @Test
    fun `非法正则不抛异常`() {
        assertNull(ReaderSearchMatcher.find("海边有座灯塔。", "[未闭合", request(isRegex = true)))
    }

    @Test
    fun `零直达长度回退查询长度`() {
        assertEquals(
            ReaderSearchMatch(4, 2),
            ReaderSearchMatcher.find("海边有座灯塔。", "灯塔", request(directIndex = 4)),
        )
    }
}
