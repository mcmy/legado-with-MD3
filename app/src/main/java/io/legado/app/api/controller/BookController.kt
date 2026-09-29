package io.legado.app.api.controller

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.Glide
import io.legado.app.api.ReturnData
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookProgress
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.comparePositionTo
import io.legado.app.data.entities.readRecord.ReadRecordSession
import io.legado.app.data.repository.ReadRecordRepository
import io.legado.app.help.AppWebDav
import io.legado.app.help.CacheManager
import io.legado.app.help.book.BookHelp
import io.legado.app.help.book.ContentProcessor
import io.legado.app.help.book.isLocal
import io.legado.app.domain.gateway.BookshelfSettingsGateway
import io.legado.app.help.glide.ImageLoader
import io.legado.app.model.BookCover
import io.legado.app.model.ImageProvider
import io.legado.app.model.ReadBook
import io.legado.app.model.localBook.LocalBook
import io.legado.app.model.webBook.WebBook
import io.legado.app.utils.GSON
import io.legado.app.utils.cnCompare
import io.legado.app.utils.fromJsonObject
import io.legado.app.utils.printOnDebug
import io.legado.app.utils.stackTraceStr
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import splitties.init.appCtx
import org.koin.core.context.GlobalContext
import java.io.File
import java.util.WeakHashMap
import java.util.concurrent.TimeUnit

object BookController {

    private val bookshelfGateway by lazy { GlobalContext.get().get<BookshelfSettingsGateway>() }
    private val readRecordRepository by lazy { GlobalContext.get().get<ReadRecordRepository>() }

    private lateinit var book: Book
    private var bookSource: BookSource? = null
    private var bookUrl: String = ""
    private val defaultCoverCache by lazy { WeakHashMap<Drawable, Bitmap>() }
    private val saveBookProgressMutex = Mutex()

    /**
     * 书架所有书籍
     */
    val bookshelf: ReturnData
        get() {
            val books = appDb.bookDao.all
            val returnData = ReturnData()
            return if (books.isEmpty()) {
                returnData.setErrorMsg("还没有添加小说")
            } else {
                val data = when (bookshelfGateway.currentSettings.bookshelfSort) {
                    1 -> books.sortedByDescending { it.latestChapterTime }
                    2 -> books.sortedWith { o1, o2 ->
                        o1.name.cnCompare(o2.name)
                    }

                    3 -> books.sortedBy { it.order }
                    else -> books.sortedByDescending { it.durChapterTime }
                }
                returnData.setData(data)
            }
        }

    /**
     * 获取封面
     */
    fun getCover(parameters: Map<String, List<String>>): ReturnData {
        val returnData = ReturnData()
        val coverPath = parameters["path"]?.firstOrNull()
        val ftBitmap = ImageLoader.loadBitmap(appCtx, coverPath)
            .override(84, 112)
            .centerCrop()
            .submit()
        return try {
            returnData.setData(ftBitmap.get(3, TimeUnit.SECONDS))
        } catch (e: Exception) {
            try {
                val defaultBitmap = defaultCoverCache.getOrPut(BookCover.defaultDrawable) {
                    Glide.with(appCtx)
                        .asBitmap()
                        .load(BookCover.defaultDrawable.toBitmap())
                        .override(84, 112)
                        .centerCrop()
                        .submit()
                        .get()
                }
                returnData.setData(defaultBitmap)
            } catch (e: Exception) {
                returnData.setErrorMsg(e.localizedMessage ?: "getCover error")
            }
        }
    }

    /**
     * 获取正文图片
     */
    fun getImg(parameters: Map<String, List<String>>): ReturnData {
        val returnData = ReturnData()
        val bookUrl = parameters["url"]?.firstOrNull()
            ?: return returnData.setErrorMsg("bookUrl为空")
        val src = parameters["path"]?.firstOrNull()
            ?: return returnData.setErrorMsg("图片链接为空")
        val width = parameters["width"]?.firstOrNull()?.toInt() ?: 640
        if (this.bookUrl != bookUrl) {
            this.book = appDb.bookDao.getBook(bookUrl)
                ?: return returnData.setErrorMsg("bookUrl不对")
            this.bookSource = appDb.bookSourceDao.getBookSource(book.origin)
        }
        this.bookUrl = bookUrl
        val bitmap = runBlocking {
            ImageProvider.cacheImage(book, src, bookSource)
            ImageProvider.getImage(book, src, width)
        }
        return returnData.setData(bitmap)
    }

    /**
     * 更新目录
     */
    fun refreshToc(parameters: Map<String, List<String>>): ReturnData {
        val returnData = ReturnData()
        try {
            val bookUrl = parameters["url"]?.firstOrNull()
            if (bookUrl.isNullOrEmpty()) {
                return returnData.setErrorMsg("参数url不能为空，请指定书籍地址")
            }
            val book = appDb.bookDao.getBook(bookUrl)
                ?: return returnData.setErrorMsg("未在数据库找到对应书籍，请先添加")
            if (book.isLocal) {
                val toc = LocalBook.getChapterList(book)
                appDb.bookChapterDao.delByBook(book.bookUrl)
                appDb.bookChapterDao.insert(*toc.toTypedArray())
                appDb.bookDao.update(book)
                return returnData.setData(toc)
            } else {
                val bookSource = appDb.bookSourceDao.getBookSource(book.origin)
                    ?: return returnData.setErrorMsg("未找到对应书源,请换源")
                val toc = runBlocking {
                    if (book.tocUrl.isBlank()) {
                        WebBook.getBookInfoAwait(bookSource, book)
                    }
                    WebBook.getChapterListAwait(bookSource, book).getOrThrow()
                }
                appDb.bookChapterDao.delByBook(book.bookUrl)
                appDb.bookChapterDao.insert(*toc.toTypedArray())
                appDb.bookDao.update(book)
                return returnData.setData(toc)
            }
        } catch (e: Exception) {
            return returnData.setErrorMsg(e.localizedMessage ?: "refresh toc error")
        }
    }

    /**
     * 获取目录
     */
    fun getChapterList(parameters: Map<String, List<String>>): ReturnData {
        val bookUrl = parameters["url"]?.firstOrNull()
        val returnData = ReturnData()
        if (bookUrl.isNullOrEmpty()) {
            return returnData.setErrorMsg("参数url不能为空，请指定书籍地址")
        }
        val chapterList = appDb.bookChapterDao.getChapterList(bookUrl)
        if (chapterList.isEmpty()) {
            return refreshToc(parameters)
        }
        return returnData.setData(chapterList)
    }

    /**
     * 获取正文
     */
    fun getBookContent(parameters: Map<String, List<String>>): ReturnData {
        val bookUrl = parameters["url"]?.firstOrNull()
        val index = parameters["index"]?.firstOrNull()?.toInt()
        val returnData = ReturnData()
        if (bookUrl.isNullOrEmpty()) {
            return returnData.setErrorMsg("参数url不能为空，请指定书籍地址")
        }
        if (index == null) {
            return returnData.setErrorMsg("参数index不能为空, 请指定目录序号")
        }
        val book = appDb.bookDao.getBook(bookUrl)
        val chapter = runBlocking {
            var chapter = appDb.bookChapterDao.getChapter(bookUrl, index)
            var wait = 0
            while (chapter == null && wait < 30) {
                delay(1000)
                chapter = appDb.bookChapterDao.getChapter(bookUrl, index)
                wait++
            }
            chapter
        }
        if (book == null || chapter == null) {
            return returnData.setErrorMsg("未找到")
        }
        var content: String? = BookHelp.getContent(book, chapter)
        if (content != null) {
            val contentProcessor = ContentProcessor.get(book.name, book.origin)
            content = runBlocking {
                contentProcessor.getContent(book, chapter, content, includeTitle = false)
                    .toString()
            }
            return returnData.setData(content)
        }
        val bookSource = appDb.bookSourceDao.getBookSource(book.origin)
            ?: return returnData.setErrorMsg("未找到书源")
        try {
            content = runBlocking {
                WebBook.getContentAwait(bookSource, book, chapter).let {
                    val contentProcessor = ContentProcessor.get(book.name, book.origin)
                    contentProcessor.getContent(book, chapter, it, includeTitle = false)
                        .toString()
                }
            }
            returnData.setData(content)
        } catch (e: Exception) {
            returnData.setErrorMsg(e.stackTraceStr)
        }
        return returnData
    }

    /**
     * 保存书籍
     */
    suspend fun saveBook(postData: String?): ReturnData {
        val returnData = ReturnData()
        GSON.fromJsonObject<Book>(postData).getOrNull()?.let { book ->
            AppWebDav.uploadBookProgress(book)
            book.save()
            return returnData.setData("")
        }
        return returnData.setErrorMsg("格式不对")
    }

    /**
     * 删除书籍
     */
    fun deleteBook(postData: String?): ReturnData {
        val returnData = ReturnData()
        GSON.fromJsonObject<Book>(postData).getOrNull()?.let { book ->
            book.delete()
            return returnData.setData("")
        }
        return returnData.setErrorMsg("格式不对")
    }

    /**
     * 保存进度
     */
    suspend fun saveBookProgress(postData: String?): ReturnData {
        val returnData = ReturnData()
        val bookProgress = GSON.fromJsonObject<BookProgress>(postData)
            .onFailure { it.printOnDebug() }
            .getOrNull()
            ?: return returnData.setErrorMsg("格式不对")

        return saveBookProgressMutex.withLock {
            val bookDao = appDb.bookDao
            val book = bookDao.getBook(bookProgress.name, bookProgress.author)
                ?: return@withLock returnData.setErrorMsg("未找到书籍")
            val activeBook = ReadBook.book?.takeIf {
                it.name == bookProgress.name && it.author == bookProgress.author
            }
            val localProgress = latestLocalProgress(book, activeBook)
            val localOrder = bookProgress.comparePositionTo(
                localProgress.durChapterIndex,
                localProgress.durChapterPos,
            )
            if (localOrder < 0) {
                return@withLock returnData.setErrorMsg(
                    WEB_PROGRESS_CONFLICT,
                    localProgress,
                )
            }
            if (localOrder == 0) {
                return@withLock returnData.setData(WEB_PROGRESS_UNCHANGED)
            }

            val updatedRows = bookDao.updateProgressIfAhead(
                book.bookUrl,
                bookProgress.durChapterIndex,
                bookProgress.durChapterPos,
                bookProgress.durChapterTitle,
                bookProgress.durChapterTime,
            )
            if (updatedRows == 0) {
                val currentBook = bookDao.getBook(book.bookUrl)
                    ?: return@withLock returnData.setErrorMsg("未找到书籍")
                val currentProgress = latestLocalProgress(currentBook, activeBook)
                val order = bookProgress.comparePositionTo(
                    currentProgress.durChapterIndex,
                    currentProgress.durChapterPos,
                )
                return@withLock when {
                    order < 0 -> returnData.setErrorMsg(
                        WEB_PROGRESS_CONFLICT,
                        currentProgress,
                    )
                    else -> returnData.setData(WEB_PROGRESS_UNCHANGED)
                }
            }

            activeBook?.let {
                val activeProgress = latestLocalProgress(book, activeBook)
                val activeOrder = bookProgress.comparePositionTo(
                    activeProgress.durChapterIndex,
                    activeProgress.durChapterPos,
                )
                if (activeOrder < 0) {
                    return@withLock returnData.setErrorMsg(
                        WEB_PROGRESS_CONFLICT,
                        activeProgress,
                    )
                }
                if (activeOrder == 0) {
                    return@withLock returnData.setData(WEB_PROGRESS_UNCHANGED)
                }
            }

            AppWebDav.uploadBookProgress(bookProgress) {
                bookDao.updateSyncTime(book.bookUrl, System.currentTimeMillis())
            }
            activeBook?.let {
                val isStillAhead = bookProgress.comparePositionTo(
                    ReadBook.durChapterIndex,
                    ReadBook.durChapterPos,
                ) > 0
                if (isStillAhead) {
                    ReadBook.webBookProgress = bookProgress
                }
            }
            return@withLock returnData.setData(WEB_PROGRESS_UPDATED)
        }
    }

    /** 保存 Web 阅读器产生的一个非重叠阅读时段。 */
    suspend fun saveReadSession(postData: String?): ReturnData {
        val request = GSON.fromJsonObject<WebReadSessionRequest>(postData)
            .onFailure { it.printOnDebug() }
            .getOrNull()
            ?: return ReturnData().setErrorMsg("格式不对")
        if (request.bookName.isBlank()) {
            return ReturnData().setErrorMsg("书名不能为空")
        }
        val duration = request.endTime - request.startTime
        if (duration !in MIN_WEB_READ_SESSION_MS..MAX_WEB_READ_SESSION_MS) {
            return ReturnData().setErrorMsg("阅读时段无效")
        }
        readRecordRepository.saveReadSession(
            ReadRecordSession(
                deviceId = "web",
                bookName = request.bookName,
                bookAuthor = request.bookAuthor,
                bookUrl = request.bookUrl,
                startTime = request.startTime,
                endTime = request.endTime,
                words = request.chapterIndex.toLong(),
            )
        )
        return ReturnData().setData("updated")
    }

    /**
     * 添加本地书籍
     */
    fun addLocalBook(
        parameters: Map<String, List<String>>,
        files: Map<String, String>
    ): ReturnData {
        val returnData = ReturnData()
        val fileName = parameters["fileName"]?.firstOrNull()
            ?: return returnData.setErrorMsg("fileName 不能为空")
        val fileData = files["fileData"]
            ?: return returnData.setErrorMsg("fileData 不能为空")
        kotlin.runCatching {
            val uri = LocalBook.saveBookFile(File(fileData).inputStream(), fileName)
            LocalBook.importFile(uri)
        }.onFailure {
            return when (it) {
                is SecurityException -> returnData.setErrorMsg("需重新设置书籍保存位置!")
                else -> returnData.setErrorMsg("保存书籍错误\n${it.localizedMessage}")
            }
        }
        return returnData.setData(true)
    }

    /**
     * 保存web阅读界面配置
     */
    fun saveWebReadConfig(postData: String?): ReturnData {
        val returnData = ReturnData()
        postData?.let {
            CacheManager.put("webReadConfig", postData)
        } ?: CacheManager.delete("webReadConfig")
        return returnData.setData("")
    }

    /**
     * 获取web阅读界面配置
     */
    fun getWebReadConfig(): ReturnData {
        val returnData = ReturnData()
        val data = CacheManager.get("webReadConfig")
            ?: return returnData.setErrorMsg("没有配置")
        return returnData.setData(data)
    }

    private fun latestLocalProgress(book: Book, activeBook: Book?): BookProgress {
        val databaseProgress = BookProgress(book)
        if (activeBook == null) return databaseProgress
        val activeProgress = BookProgress(
            name = activeBook.name,
            author = activeBook.author,
            durChapterIndex = ReadBook.durChapterIndex,
            durChapterPos = ReadBook.durChapterPos,
            durChapterTime = activeBook.durChapterTime,
            durChapterTitle = activeBook.durChapterTitle,
        )
        return if (activeProgress.comparePositionTo(
                databaseProgress.durChapterIndex,
                databaseProgress.durChapterPos,
            ) > 0
        ) {
            activeProgress
        } else {
            databaseProgress
        }
    }

    private data class WebReadSessionRequest(
        val bookName: String,
        val bookAuthor: String,
        val bookUrl: String,
        val startTime: Long,
        val endTime: Long,
        val chapterIndex: Int,
    )

    const val WEB_PROGRESS_CONFLICT = "web_progress_conflict"
    private const val WEB_PROGRESS_UPDATED = "updated"
    private const val WEB_PROGRESS_UNCHANGED = "unchanged"
    private const val MIN_WEB_READ_SESSION_MS = 10_000L
    private const val MAX_WEB_READ_SESSION_MS = 90_000L

}
