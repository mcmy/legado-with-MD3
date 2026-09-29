package io.legado.app.domain.model.settings

fun nextQuickStyleIndex(current: Int, selected: List<Boolean>): Int? {
    if (selected.isEmpty()) return null
    val indices = selected.indices.filter { selected[it] }
    if (indices.isEmpty()) return if (selected.size > 1 && current == 0) 1 else 0
    return indices.firstOrNull { it > current } ?: indices.first()
}
