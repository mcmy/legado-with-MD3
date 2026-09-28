package io.legado.app.ui.main

import android.app.Application
import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class MainIntentTest {

    @Test
    fun `media control reader reuses task and requests home parent`() {
        val context: Application = RuntimeEnvironment.getApplication()

        val intent = MainIntent.createReadBookMediaControlIntent(context)

        val expectedFlags = Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_SINGLE_TOP
        assertEquals(expectedFlags, intent.flags and expectedFlags)
        assertEquals(
            MainRouteConst.ROUTE_READ_BOOK,
            intent.getStringExtra(MainIntent.EXTRA_START_ROUTE),
        )
        assertTrue(intent.getBooleanExtra(MainIntent.EXTRA_READ_ALOUD, false))
        assertTrue(MainIntent.shouldOpenRouteWithHomeParent(intent))
    }

    @Test
    fun `regular reader keeps existing activity and navigation semantics`() {
        val context: Application = RuntimeEnvironment.getApplication()

        val intent = MainIntent.createReadBookIntent(context, readAloud = true)

        val mediaControlFlags = Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_SINGLE_TOP
        assertEquals(0, intent.flags and mediaControlFlags)
        assertFalse(MainIntent.shouldOpenRouteWithHomeParent(intent))
    }
}
