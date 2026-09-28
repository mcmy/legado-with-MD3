package io.legado.app.data.repository

import android.app.Application
import androidx.room.Room
import io.legado.app.data.AppDatabase
import io.legado.app.data.entities.readRecord.ReadRecord
import io.legado.app.data.entities.readRecord.ReadRecordSession
import io.legado.app.help.config.AppConfigStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * 阅读统计口径回归测试：
 *
 * 1. 阅读总览「总」模式与阅读记录页、首页的总时长同源（readRecord 汇总，包含旧版遗留时长），
 *    三者保持一致；旧版只有 readRecord、没有 readRecordDetail 的时长不再被总览漏掉。
 * 2. 阅读字数（readRecordDetail.readWords）是章节序号的累加，不作为「阅读字数」展示，
 *    总览统计卡片不再显示该误导性指标。
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class ReadRecordStatisticsConsistencyTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: ReadRecordRepository

    @Before
    fun setUp() {
        AppConfigStore.init(RuntimeEnvironment.getApplication())
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            AppDatabase::class.java
        )
            .allowMainThreadQueries().build()
        repository = ReadRecordRepository(database.readRecordDao, database, SettingsRepository())
    }

    @After
    fun tearDown() = database.close()

    // ---------- 问题 1：总览（readRecordDetail）与记录（readRecord）总时长不一致 ----------

    @Test
    fun `records total equals overview total in pure session flow`() = runBlocking {
        val day = "2026-01-02"
        val base = day.toEpochDay() * 86_400_000L
        repository.saveReadSession(session(base, base + 100_000, words = 45))
        repository.saveReadSession(session(base + 200_000, base + 300_000, words = 46))

        val recordsTotal = repository.getTotalReadTime().first()
        val overviewTotal = repository.getAllRecordDetails("").first().sumOf { it.readTime }

        assertEquals(200_000L, recordsTotal)
        assertEquals(recordsTotal, overviewTotal)
    }

    @Test
    fun `legacy record time is missing from overview total`() = runBlocking {
        // 旧版本/上游备份只有 readRecord.readTime，没有 readRecordDetail（升级前无此表）。
        database.readRecordDao.insert(
            ReadRecord(
                deviceId,
                bookName,
                author,
                readTime = 3_600_000L,
                lastRead = 1_700_000_000_000L
            )
        )

        val recordsTotal = repository.getTotalReadTime().first() // 阅读记录页「累计阅读成就」
        val overviewTotal =
            repository.getAllRecordDetails("").first().sumOf { it.readTime } // 阅读总览「阅读时间」

        assertEquals(3_600_000L, recordsTotal)
        assertEquals(0L, overviewTotal)
        // 两个页面展示的累计阅读时长不一致（记录 > 总览）。
    }

    @Test
    fun `cross-device synced duplicates inflate totals but sessions dedupe`() = runBlocking {
        // 同一本书在设备 A/B 各有一条同步副本（同一时段的 session 与 detail 都重复）。
        // 记录汇总与详情聚合都会把同步副本算两遍；会话时间线按内容去重。
        // 阅读总览「总」模式与记录页同源（readRecord 汇总），两者保持一致；
        // 同步副本的重复时长由「修复」流程（repairDuplicateSessions）清理。
        val day = "2026-01-02"
        val base = day.toEpochDay() * 86_400_000L
        val sessionA = session(base, base + 100_000, words = 45, device = "device-a")
        val sessionB = sessionA.copy(deviceId = "device-b")
        repository.saveReadSession(sessionA)
        repository.saveReadSession(sessionB)

        val recordsTotal = repository.getTotalReadTime().first() // 记录页总时长（与总览「总」同源）
        val overviewTotal = repository.getAllRecordDetails("").first().sumOf { it.readTime } // 详情聚合
        val sessionsTotal = repository.getAllSessions().first()
            .sumOf { it.endTime - it.startTime } // 时间线/热力图：按内容去重后的会话

        assertEquals(200_000L, recordsTotal)
        assertEquals(200_000L, overviewTotal)
        assertEquals(100_000L, sessionsTotal)
    }

    // ---------- 问题 2：阅读字数 = 章节序号之和，不是字数 ----------

    @Test
    fun `overview words are summed chapter indexes not word counts`() = runBlocking {
        val day = "2026-01-02"
        val base = day.toEpochDay() * 86_400_000L
        // 读者在章节 45、46、47 各读了一段（words 字段 = durChapterIndex）。
        repository.saveReadSession(session(base, base + 60_000, words = 45))
        repository.saveReadSession(session(base + 100_000, base + 160_000, words = 46))
        repository.saveReadSession(session(base + 200_000, base + 260_000, words = 47))

        val merged = repository.getAllRecordDetails("").first()
        assertEquals(180_000L, merged.sumOf { it.readTime })
        // 当天实际阅读 3 个章节，却显示"阅读字数"为章节序号之和 45+46+47=138。
        assertEquals(138L, merged.sumOf { it.readWords })
    }

    private fun session(
        start: Long,
        end: Long,
        words: Long,
        device: String = deviceId,
    ) = ReadRecordSession(
        deviceId = device,
        bookName = bookName,
        bookAuthor = author,
        startTime = start,
        endTime = end,
        words = words,
    )

    private fun String.toEpochDay(): Long = java.time.LocalDate.parse(this).toEpochDay()

    private companion object {
        const val deviceId = "device"
        const val bookName = "book"
        const val author = "author"
    }
}
