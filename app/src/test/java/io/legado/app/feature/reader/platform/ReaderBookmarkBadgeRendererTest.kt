package io.legado.app.feature.reader.platform

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import io.legado.app.feature.reader.core.model.ReaderBookmarkBadge
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReaderBookmarkBadgeRendererTest {
    @get:Rule val folder = TemporaryFolder()
    private val badge = ReaderBookmarkBadge(10f, 10f, 24, 48)

    @Test fun defaultRibbonPreservesYellowBodyAndBottomNotch() {
        val bitmap = Bitmap.createBitmap(60, 80, Bitmap.Config.ARGB_8888)
        ReaderBookmarkBadgeRenderer.draw(Canvas(bitmap), badge, null)
        assertEquals(0xffffc107.toInt(), bitmap.getPixel(22, 30))
        assertEquals(Color.TRANSPARENT, bitmap.getPixel(22, 51))
        assertEquals(Color.TRANSPARENT, bitmap.getPixel(11, 30))
        assertEquals(Color.TRANSPARENT, bitmap.getPixel(22, 59))
    }

    @Test fun defaultRibbonMatchesExistingVectorPixels() {
        val expected = Bitmap.createBitmap(60, 80, Bitmap.Config.ARGB_8888)
        val actual = Bitmap.createBitmap(60, 80, Bitmap.Config.ARGB_8888)
        val drawable = RuntimeEnvironment.getApplication().getDrawable(io.legado.app.R.drawable.ic_bookmark_badge)!!
        drawable.setBounds(10, 10, 34, 58)
        drawable.draw(Canvas(expected))
        ReaderBookmarkBadgeRenderer.draw(Canvas(actual), badge, null)
        val expectedPixels = IntArray(60 * 80)
        val actualPixels = IntArray(60 * 80)
        expected.getPixels(expectedPixels, 0, 60, 0, 0, 60, 80)
        actual.getPixels(actualPixels, 0, 60, 0, 0, 60, 80)
        assertArrayEquals(expectedPixels, actualPixels)
    }

    @Test fun customBitmapUsesFitCenterAndFollowsPageTranslation() {
        val source = Bitmap.createBitmap(20, 10, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.translate(20f, 5f)
        ReaderBookmarkBadgeRenderer.draw(canvas, badge, source)
        assertEquals(Color.RED, bitmap.getPixel(42, 39))
        assertEquals(Color.TRANSPARENT, bitmap.getPixel(42, 20))
        assertEquals(Color.TRANSPARENT, bitmap.getPixel(22, 34))
    }

    @Test fun invalidImageFallsBackAndSvgCanBeUpscaled() {
        val invalid = folder.newFile("invalid.svg").apply { writeText("not an image") }
        assertNull(ReaderBookmarkBadgeRenderer.load(badge.copy(imageSource = invalid.path)))
        val svg = folder.newFile("badge.svg").apply {
            writeText("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 4 2"><rect width="4" height="2" fill="#ff0000"/></svg>""")
        }
        val image = ReaderBookmarkBadgeRenderer.load(badge.copy(imageSource = svg.path))!!
        assertEquals(24, image.width)
        assertEquals(12, image.height)
        assertEquals(Color.RED, image.getPixel(12, 6))
    }

    @Test fun cacheReusesImagesButReloadsSamePathWhenVersionOrSizeChanges() {
        val file = folder.newFile("badge.png")
        fun write(color: Int) {
            val bitmap = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        write(Color.RED)
        val first = badge.copy(imageSource = file.path, imageVersion = "1")
        val image = ReaderBookmarkBadgeRenderer.load(first)!!
        assertSame(image, ReaderBookmarkBadgeRenderer.load(first.copy(leftPx = 30f)))
        write(Color.BLUE)
        val changed = first.copy(imageVersion = "2")
        val reloaded = ReaderBookmarkBadgeRenderer.load(changed)!!
        assertEquals(Color.BLUE, reloaded.getPixel(0, 0))
        assertNotSame(image, reloaded)
        assertNotSame(reloaded, ReaderBookmarkBadgeRenderer.load(changed.copy(widthPx = 40, heightPx = 80)))
        assertFalse(image.isRecycled)
    }
}
