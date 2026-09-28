package io.legado.app.feature.reader.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.caverock.androidsvg.SVG
import io.legado.app.feature.reader.core.model.ReaderBookmarkBadge
import java.io.File
import kotlin.math.min

/** Android decoding stays outside the drawing pass. A single custom badge is shared by the page window. */
object ReaderBookmarkBadgeRenderer {
    private data class ImageKey(val source: String, val version: String, val width: Int, val height: Int)
    private var cachedKey: ImageKey? = null
    private var cachedBitmap: Bitmap? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val ribbon = Path().apply {
        // Same 12 x 24 viewport as ic_bookmark_badge.xml.
        moveTo(2f, 2f)
        lineTo(10f, 2f)
        lineTo(10f, 22f)
        lineTo(6f, 17f)
        lineTo(2f, 22f)
        close()
    }

    /** Call on an IO dispatcher, never from Canvas.draw. Invalid custom files use the default ribbon. */
    @Synchronized
    fun load(badge: ReaderBookmarkBadge): Bitmap? {
        val key = ImageKey(badge.imageSource, badge.imageVersion, badge.widthPx, badge.heightPx)
        if (key == cachedKey) return cachedBitmap
        val bitmap = badge.imageSource.takeIf(String::isNotBlank)?.let { path ->
            runCatching {
                val file = File(path)
                if (!file.isFile) return@runCatching null
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(path, bounds)
                if (bounds.outWidth > 0 && bounds.outHeight > 0) {
                    var sample = 1
                    while (bounds.outWidth / (sample * 2) >= badge.widthPx * 2 &&
                        bounds.outHeight / (sample * 2) >= badge.heightPx * 2
                    ) sample *= 2
                    BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
                } else {
                    val svg = file.inputStream().use(SVG::getFromInputStream)
                    val width = svg.documentWidth.takeIf { it > 0 } ?: svg.documentViewBox?.width() ?: 0f
                    val height = svg.documentHeight.takeIf { it > 0 } ?: svg.documentViewBox?.height() ?: 0f
                    if (width <= 0 || height <= 0) return@runCatching null
                    val scale = min(badge.widthPx / width, badge.heightPx / height)
                    Bitmap.createBitmap((width * scale).toInt().coerceAtLeast(1),
                        (height * scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888).also {
                        if (svg.documentViewBox == null) svg.setDocumentViewBox(0f, 0f, width, height)
                        svg.setDocumentWidth("100%")
                        svg.setDocumentHeight("100%")
                        svg.renderToCanvas(Canvas(it))
                    }
                }
            }.getOrNull()
        }
        // Do not recycle the previous bitmap: a leaving page may still be drawing it.
        cachedKey = key
        cachedBitmap = bitmap
        return bitmap
    }

    /** Returns null while this badge identity has not been decoded; a pair with null bitmap means decoding failed. */
    @Synchronized
    fun cachedResult(badge: ReaderBookmarkBadge): Pair<ReaderBookmarkBadge, Bitmap?>? {
        val key = ImageKey(badge.imageSource, badge.imageVersion, badge.widthPx, badge.heightPx)
        return if (key == cachedKey) badge to cachedBitmap?.takeUnless(Bitmap::isRecycled) else null
    }

    fun draw(canvas: Canvas, badge: ReaderBookmarkBadge, bitmap: Bitmap?) {
        val checkpoint = canvas.save()
        try {
            canvas.translate(badge.leftPx, badge.topPx)
            canvas.clipRect(0f, 0f, badge.widthPx.toFloat(), badge.heightPx.toFloat())
            if (bitmap != null && !bitmap.isRecycled) {
                val scale = min(badge.widthPx.toFloat() / bitmap.width, badge.heightPx.toFloat() / bitmap.height)
                val width = bitmap.width * scale
                val height = bitmap.height * scale
                val left = (badge.widthPx - width) / 2
                val top = (badge.heightPx - height) / 2
                canvas.drawBitmap(bitmap, null, RectF(left, top, left + width, top + height), paint)
            } else {
                canvas.scale(badge.widthPx / 12f, badge.heightPx / 24f)
                paint.color = 0xffffc107.toInt()
                canvas.drawPath(ribbon, paint)
            }
        } finally {
            canvas.restoreToCount(checkpoint)
        }
    }
}
