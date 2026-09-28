package io.legado.app.domain.usecase.readRecord

import io.legado.app.data.entities.readRecord.ReadRecord
import io.legado.app.data.entities.readRecord.ReadRecordDetail
import io.legado.app.ui.book.readRecord.ReadPeriod
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class GetReadRecordOverviewUseCaseTest {

    private val useCase = GetReadRecordOverviewUseCase()

    @Test
    fun `ALL mode total time uses merged record totals including legacy`() {
        // 每日详情只有新版本数据；旧版遗留时长只存在于 readRecord。
        val details = listOf(
            detail("2026-01-02", readTime = 100_000L),
            detail("2026-01-03", readTime = 200_000L),
        )
        val records = listOf(
            ReadRecord("", "book", "author", readTime = 800_000L, lastRead = 0L)
        )

        val state = useCase(ReadPeriod.ALL, LocalDate.of(2026, 1, 3), details, records, emptyList())

        // 与阅读记录页/首页的总时长同源：100_000 + 200_000 + 遗留 500_000。
        assertEquals(800_000L, state.totalTime)
    }

    @Test
    fun `period mode total time uses filtered details`() {
        val details = listOf(
            detail("2026-01-02", readTime = 100_000L),
            detail("2026-02-02", readTime = 200_000L),
        )
        val records = listOf(
            ReadRecord("", "book", "author", readTime = 300_000L, lastRead = 0L)
        )

        val state =
            useCase(ReadPeriod.MONTH, LocalDate.of(2026, 1, 15), details, records, emptyList())

        // 周期视图没有日期的旧版时长无法归属，仍按详情统计。
        assertEquals(100_000L, state.totalTime)
    }

    private fun detail(date: String, readTime: Long) = ReadRecordDetail(
        deviceId = "",
        bookName = "book",
        bookAuthor = "author",
        date = date,
        readTime = readTime,
        readWords = 0L,
    )
}
