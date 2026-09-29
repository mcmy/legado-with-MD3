package io.legado.app.data.entities

data class BookProgress(
    val name: String,
    val author: String,
    val durChapterIndex: Int,
    val durChapterPos: Int,
    val durChapterTime: Long,
    val durChapterTitle: String?
) {

    constructor(book: Book) : this(
        name = book.name,
        author = book.author,
        durChapterIndex = book.durChapterIndex,
        durChapterPos = book.durChapterPos,
        durChapterTime = book.durChapterTime,
        durChapterTitle = book.durChapterTitle
    )

}

fun BookProgress.comparePositionTo(chapterIndex: Int, chapterPos: Int): Int {
    return when {
        durChapterIndex != chapterIndex -> durChapterIndex.compareTo(chapterIndex)
        else -> durChapterPos.compareTo(chapterPos)
    }
}
