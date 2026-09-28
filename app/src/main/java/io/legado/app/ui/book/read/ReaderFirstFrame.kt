package io.legado.app.ui.book.read

import android.os.SystemClock
import android.util.Log
import io.legado.app.BuildConfig
import java.util.concurrent.atomic.AtomicBoolean

internal const val EXTRA_FIRST_FRAME_STARTED_AT_NANOS = "readerFirstFrameStartedAtNanos"
internal const val READER_RENDERER_NAME = "compose-canvas"

internal enum class ReaderStartupFramePhase(val logValue: String) {
    LOADING("loading"),
    CONTENT("content"),
}

internal fun readerFirstFrameLogMessage(
    durationMillis: Float,
    phase: ReaderStartupFramePhase = ReaderStartupFramePhase.CONTENT,
): String = "renderer=$READER_RENDERER_NAME phase=${phase.logValue} durationMs=$durationMillis"

/**
 * Debug-only 首帧探针：从 intent 传入的启动时刻到正文第一次绘制之间的耗时，
 * 通过 logcat tag `ReaderFirstFrame` 输出，供 tools 侧脚本采集。
 */
internal class ReaderFirstFrameTracker(
    private val startedAtNanos: Long,
) {
    private val reported = AtomicBoolean(false)

    fun report(phase: ReaderStartupFramePhase = ReaderStartupFramePhase.CONTENT) {
        if (!BuildConfig.DEBUG) return
        if (!reported.compareAndSet(false, true)) return
        val durationMillis = (SystemClock.elapsedRealtimeNanos() - startedAtNanos) / 1_000_000f
        Log.i("ReaderFirstFrame", readerFirstFrameLogMessage(durationMillis, phase))
    }
}
