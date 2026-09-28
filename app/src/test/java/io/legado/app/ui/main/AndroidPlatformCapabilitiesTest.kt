package io.legado.app.ui.main

import org.junit.Assert.assertEquals
import org.junit.Test

class AndroidPlatformCapabilitiesTest {

    @Test fun selectCornerRadiusTakesMinOfNonZeroCorners() {
        assertEquals(
            80f,
            AndroidPlatformCapabilities.selectCornerRadiusPx(listOf(0f, 100f, 80f, 120f)),
            .001f,
        )
    }

    @Test fun selectCornerRadiusIgnoresReportedZeroCorners() {
        assertEquals(
            100f,
            AndroidPlatformCapabilities.selectCornerRadiusPx(listOf(100f, 0f, 0f)),
            .001f,
        )
    }

    @Test fun selectCornerRadiusFallsBackWhenNoCornerIsReported() {
        assertEquals(
            AndroidPlatformCapabilities.DEFAULT_DISPLAY_CORNER_RADIUS_PX,
            AndroidPlatformCapabilities.selectCornerRadiusPx(emptyList()),
            .001f,
        )
        assertEquals(
            AndroidPlatformCapabilities.DEFAULT_DISPLAY_CORNER_RADIUS_PX,
            AndroidPlatformCapabilities.selectCornerRadiusPx(listOf(0f, 0f, 0f, 0f)),
            .001f,
        )
    }

    @Test fun selectCornerRadiusHonorsCustomFallback() {
        assertEquals(24f, AndroidPlatformCapabilities.selectCornerRadiusPx(emptyList(), fallback = 24f), .001f)
    }
}
