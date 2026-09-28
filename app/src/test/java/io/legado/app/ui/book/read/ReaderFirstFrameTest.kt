package io.legado.app.ui.book.read

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ReaderFirstFrameTest {
    @Test
    fun `first frame telemetry identifies the compose canvas renderer`() {
        val message = readerFirstFrameLogMessage(12.5f)

        assertEquals("renderer=compose-canvas phase=content durationMs=12.5", message)
        assertFalse(message.contains("legacy"))
    }

    @Test
    fun `loading frame telemetry is distinguishable from content`() {
        assertEquals(
            "renderer=compose-canvas phase=loading durationMs=3.0",
            readerFirstFrameLogMessage(3f, ReaderStartupFramePhase.LOADING),
        )
    }
}
