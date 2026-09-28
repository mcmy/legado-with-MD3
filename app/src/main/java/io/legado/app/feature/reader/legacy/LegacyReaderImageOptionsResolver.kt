package io.legado.app.feature.reader.legacy

import io.legado.app.feature.reader.core.layout.ReaderImageLayoutMode
import io.legado.app.feature.reader.core.layout.ReaderImageOptions
import io.legado.app.feature.reader.core.layout.ReaderImageOptionsResolver
import io.legado.app.feature.reader.core.layout.ReaderTextAlignment
import io.legado.app.model.analyzeRule.AnalyzeUrl.Companion.paramPattern
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject

/** Keeps source-URL JSON parsing at the legacy input boundary; the Canvas core receives typed options. */
object LegacyReaderImageOptionsResolver : ReaderImageOptionsResolver {
    override fun resolve(source: String): ReaderImageOptions? {
        val separator = paramPattern.find(source) ?: return null
        val values = GSON.fromJsonObject<Map<String, String>>(
            source.substring(separator.range.last + 1),
        ).getOrNull() ?: return null
        val style = values["style"]?.uppercase()
        val width = values["width"]
        return ReaderImageOptions(
            layoutMode = when (style) {
                null -> null
                "TEXT" -> ReaderImageLayoutMode.INLINE
                "FULL" -> ReaderImageLayoutMode.FULL_WIDTH
                "SINGLE" -> ReaderImageLayoutMode.SINGLE_PAGE
                else -> ReaderImageLayoutMode.STANDALONE
            },
            requestedWidthPx = width?.takeUnless { it.endsWith('%') }?.toFloatOrNull()
                ?.takeIf { it > 0f },
            requestedWidthFraction = width?.takeIf { it.endsWith('%') }
                ?.dropLast(1)?.toFloatOrNull()?.div(100f)?.takeIf { it > 0f },
            horizontalAlignment = when (style) {
                "LEFT" -> ReaderTextAlignment.START
                "RIGHT" -> ReaderTextAlignment.END
                else -> null
            },
            action = values["click"]?.takeIf(String::isNotBlank),
        )
    }
}
