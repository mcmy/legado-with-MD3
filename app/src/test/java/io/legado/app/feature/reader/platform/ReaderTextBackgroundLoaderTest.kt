package io.legado.app.feature.reader.platform

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Before
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.RuntimeEnvironment
import splitties.init.injectAsAppCtx

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReaderTextBackgroundLoaderTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Before
    fun setUp() {
        RuntimeEnvironment.getApplication().injectAsAppCtx()
    }

    @Test
    fun fileDimensionsAndBitmapUseTheSameSourceResolution() {
        val file = temporaryFolder.newFile("frame.png")
        Bitmap.createBitmap(4, 6, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.RED)
            file.outputStream().use { compress(Bitmap.CompressFormat.PNG, 100, it) }
            recycle()
        }

        assertEquals(4 to 6, ReaderTextBackgroundLoader.dimensions(file.absolutePath))
        val loaded = ReaderTextBackgroundLoader.load(file.absolutePath)!!
        assertEquals(4, loaded.width)
        assertEquals(6, loaded.height)
        assertSame(loaded, ReaderTextBackgroundLoader.cached(file.absolutePath))
    }

    @Test
    fun changedFileGetsANewCacheIdentity() {
        val file = temporaryFolder.newFile("changing.png")
        fun write(width: Int, height: Int, color: Int) {
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                eraseColor(color)
                file.outputStream().use { compress(Bitmap.CompressFormat.PNG, 100, it) }
                recycle()
            }
            assertTrue(file.setLastModified(System.currentTimeMillis() + width * 1_000L))
        }
        write(2, 3, Color.RED)
        val first = ReaderTextBackgroundLoader.load(file.absolutePath)!!
        write(5, 4, Color.BLUE)
        val second = ReaderTextBackgroundLoader.load(file.absolutePath)!!

        assertNotSame(first, second)
        assertEquals(5 to 4, second.width to second.height)
    }

    @Test
    fun preservesLegacyAssetSourceConventions() {
        assertEquals(listOf("themes/paper.png"), ReaderTextBackgroundLoader.assetCandidates("assets://themes/paper.png"))
        assertEquals(listOf("bg/paper.png"), ReaderTextBackgroundLoader.assetCandidates("paper.png"))
        assertEquals(listOf("bg/paper.png"), ReaderTextBackgroundLoader.assetCandidates("bg/paper.png"))
        assertTrue(ReaderTextBackgroundLoader.assetCandidates("content://theme/paper").isEmpty())
    }
}
