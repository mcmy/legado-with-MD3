package io.legado.app.ui.book.info

internal data class BookInfoBackdropStyle(
    val showCover: Boolean,
    val blurCover: Boolean,
    val applySeedOverlay: Boolean,
)

internal fun resolveBookInfoBackdropStyle(backgroundMode: String): BookInfoBackdropStyle {
    // 取值与主题设置持久化的 bookInfoBackground 一致：off/ off_for_default / on
    return when (backgroundMode) {
        "off" -> BookInfoBackdropStyle(
            showCover = true,
            blurCover = false,
            applySeedOverlay = true,
        )

        "off_for_default" -> BookInfoBackdropStyle(
            showCover = false,
            blurCover = false,
            applySeedOverlay = false,
        )

        else -> BookInfoBackdropStyle(
            showCover = true,
            blurCover = true,
            applySeedOverlay = true,
        )
    }
}
