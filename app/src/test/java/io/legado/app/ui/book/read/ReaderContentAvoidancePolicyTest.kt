package io.legado.app.ui.book.read

import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderContentAvoidancePolicyTest {

    private val insets = ReaderContentAvoidancePolicy.SystemBarInsets(
        statusBarTopPx = 100,
        navigationBarBottomPx = 140,
        cutoutLeftPx = 10,
        cutoutTopPx = 120,
        cutoutRightPx = 12,
        cutoutBottomPx = 4,
    )

    @Test fun defaultConfigAvoidsBothBarsWithoutCutout() {
        val padding = ReaderContentAvoidancePolicy.padding(
            insets, hideStatusBar = false, hideNavigationBar = false,
            paddingDisplayCutouts = false, inMultiWindow = false,
        )
        assertEquals(ReaderPadding(top = 100, bottom = 140), padding)
    }

    @Test fun hideNavigationBarRemovesBottomAvoidanceOnly() {
        val padding = ReaderContentAvoidancePolicy.padding(
            insets, hideStatusBar = false, hideNavigationBar = true,
            paddingDisplayCutouts = false, inMultiWindow = false,
        )
        assertEquals(ReaderPadding(top = 100), padding)
    }

    @Test fun hideStatusBarRemovesTopAvoidanceOnly() {
        val padding = ReaderContentAvoidancePolicy.padding(
            insets, hideStatusBar = true, hideNavigationBar = false,
            paddingDisplayCutouts = false, inMultiWindow = false,
        )
        assertEquals(ReaderPadding(bottom = 140), padding)
    }

    @Test fun multiWindowForcesNoBarAvoidanceRegardlessOfConfig() {
        val padding = ReaderContentAvoidancePolicy.padding(
            insets, hideStatusBar = false, hideNavigationBar = false,
            paddingDisplayCutouts = false, inMultiWindow = true,
        )
        assertEquals(ReaderPadding(), padding)
    }

    @Test fun paddingDisplayCutoutsAddsCutoutInsetsOnAllEdges() {
        val padding = ReaderContentAvoidancePolicy.padding(
            insets, hideStatusBar = true, hideNavigationBar = true,
            paddingDisplayCutouts = true, inMultiWindow = false,
        )
        assertEquals(ReaderPadding(left = 10, top = 120, right = 12, bottom = 4), padding)
    }

    @Test fun cutoutTopIsSkippedWhileTheStatusBarPlaceholderCoversIt() {
        val padding = ReaderContentAvoidancePolicy.padding(
            insets, hideStatusBar = false, hideNavigationBar = true,
            paddingDisplayCutouts = true, inMultiWindow = false,
        )
        // 状态栏占位可见（100 >= 刘海顶 120 的原版假设由占位高度覆盖），顶部不再叠加刘海
        assertEquals(ReaderPadding(left = 10, top = 100, right = 12, bottom = 4), padding)
    }

    @Test fun policyOutputIsIndependentOfBarVisibilityTransitions() {
        // 系统栏显隐只改变平台 dispatch，不改变"忽略可见性"的采样值；
        // 同一采样 + 同一配置必须得到同一 padding（菜单开关零重排的依据）
        val before = ReaderContentAvoidancePolicy.padding(
            insets, hideStatusBar = true, hideNavigationBar = true,
            paddingDisplayCutouts = false, inMultiWindow = false,
        )
        val after = ReaderContentAvoidancePolicy.padding(
            insets, hideStatusBar = true, hideNavigationBar = true,
            paddingDisplayCutouts = false, inMultiWindow = false,
        )
        assertEquals(before, after)
        assertEquals(ReaderPadding(), before)
    }
}
