package io.legado.app.feature.reader.core.selection

data class ReaderSearchRequest(
    val directIndex: Int,
    val directLength: Int,
    val occurrence: Int,
    val isRegex: Boolean,
)

data class ReaderSearchMatch(val start: Int, val length: Int, val isTitle: Boolean = false)

/** Resolves a search result against the latest processed chapter text, without page coordinates. */
object ReaderSearchMatcher {
    /**
     * [title] is the exact title prefix used by full-text search. It stays outside the
     * renderer's body coordinate system, but is included while resolving occurrences.
     */
    fun find(
        content: String,
        query: String,
        request: ReaderSearchRequest,
        title: String? = null,
    ): ReaderSearchMatch? {
        if (query.isEmpty()) return null
        val titlePrefix = title?.let { "$it\n" }.orEmpty()
        val searchContent = titlePrefix + content
        val directLength = request.directLength.takeIf { it > 0 } ?: query.length
        val directIndex = request.directIndex.takeIf { it >= 0 }
        if (directIndex != null && directIndex + directLength <= searchContent.length) {
            val matches = if (request.isRegex) {
                runCatching {
                    Regex(query).matches(
                        searchContent.substring(
                            directIndex,
                            directIndex + directLength
                        )
                    )
                }.getOrDefault(false)
            } else {
                searchContent.regionMatches(directIndex, query, 0, query.length, ignoreCase = false)
            }
            if (matches) return matchAt(directIndex, directLength, titlePrefix.length)
        }
        if (request.isRegex) {
            return runCatching {
                Regex(query).findAll(searchContent)
                    .drop(request.occurrence.coerceAtLeast(0))
                    .firstOrNull()
                    ?.let { matchAt(it.range.first, it.value.length, titlePrefix.length) }
            }.getOrNull()
        }
        var occurrence = 0
        var index = searchContent.indexOf(query)
        while (occurrence != request.occurrence.coerceAtLeast(0) && index >= 0) {
            index = searchContent.indexOf(query, index + query.length)
            occurrence += 1
        }
        return index.takeIf { it >= 0 }?.let { matchAt(it, query.length, titlePrefix.length) }
    }

    private fun matchAt(index: Int, length: Int, titlePrefixLength: Int): ReaderSearchMatch? =
        when {
            titlePrefixLength == 0 -> ReaderSearchMatch(index, length)
            index + length <= titlePrefixLength - 1 -> ReaderSearchMatch(
                index,
                length,
                isTitle = true
            )

            index >= titlePrefixLength -> ReaderSearchMatch(index - titlePrefixLength, length)
            // A regular expression may straddle the title/body separator. It cannot be painted as
            // one Canvas range, so never substitute an unrelated visible match.
            else -> null
    }
}
