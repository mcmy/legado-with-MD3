package io.legado.app.ui.widget.components.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlParserTest {

    @Test
    fun button_withOnClickSplitsNameAndClick() {
        val doc =
            HtmlParser.parse("<p>阅读 <button>评论@onclick:showCmt(https://example.com/1)</button></p>")

        val paragraph = doc.paragraphs.single()
        val button = paragraph.content.filterIsInstance<HtmlInline.Button>().single()
        assertEquals("评论", button.name)
        assertEquals("showCmt(https://example.com/1)", button.click)
    }

    @Test
    fun buttonWithoutOnClickStaysPlainText() {
        val doc = HtmlParser.parse("<p>点击 <button>确认</button></p>")

        val paragraph = doc.paragraphs.single()
        assertTrue(paragraph.content.none { it is HtmlInline.Button })
        val text =
            paragraph.content.filterIsInstance<HtmlInline.Text>().joinToString("") { it.value }
        assertTrue(text.contains("确认"))
    }

    @Test
    fun buttonInsideDivUsehtmlStyleIsParsed() {
        val doc = HtmlParser.parse(
            "<div>评论：<button>点赞@onclick:showCmt(\"https://a.com/x\")</button></div>"
        )

        val buttons = doc.paragraphs.flatMap { it.content }.filterIsInstance<HtmlInline.Button>()
        assertEquals(1, buttons.size)
        assertEquals("点赞", buttons[0].name)
        assertEquals("showCmt(\"https://a.com/x\")", buttons[0].click)
    }

    @Test
    fun hrBecomesHorizontalRuleParagraph() {
        val doc = HtmlParser.parse("<div>a<hr>b</div>")

        assertTrue(doc.paragraphs.any { paragraph ->
            paragraph.content.size == 1 && paragraph.content[0] is HtmlInline.HorizontalRule
        })
    }

    @Test
    fun imageWithClickParamExtractsClick() {
        val doc = HtmlParser.parse(
            "<img src=\"https://a.com/1.png,{&quot;click&quot;:&quot;showCmt(1)&quot;,&quot;width&quot;:&quot;80%&quot;}\">"
        )

        val image =
            doc.paragraphs.flatMap { it.content }.filterIsInstance<HtmlInline.Image>().single()
        assertEquals("showCmt(1)", image.click)
        assertEquals("https://a.com/1.png", image.loadSource)
    }

    @Test
    fun imageWithoutClickParamKeepsClickNull() {
        val doc = HtmlParser.parse("<img src=\"https://a.com/1.png\">")

        val image =
            doc.paragraphs.flatMap { it.content }.filterIsInstance<HtmlInline.Image>().single()
        assertEquals(null, image.click)
    }

    @Test
    fun tableWithDirectCellsIsParsedWithoutScopeSelector() {
        val doc = HtmlParser.parse("<table><tr><th>Word</th><td>释义</td></tr></table>")

        val text = doc.paragraphs.single().content
            .filterIsInstance<HtmlInline.Text>()
            .joinToString("") { it.value }
        assertEquals("Word  释义", text)
    }
}
