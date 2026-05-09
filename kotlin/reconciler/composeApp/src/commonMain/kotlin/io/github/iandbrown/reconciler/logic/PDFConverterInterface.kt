package io.github.iandbrown.reconciler.logic

import dev.shivathapaa.logger.api.LoggerFactory
import io.github.iandbrown.reconciler.ui.ImportDefinitionViewModel
import kotlin.collections.iterator

interface PDFConverterInterface {
    fun rowContent(rowFilter: (Set<String>) -> Boolean): List<Map<Range, String>>
    fun getDateRange(): Pair<Int, Int>
}

data class Range(val from: Float, val to: Float) {
    override fun toString(): String {
        return "Range(${from}F, ${to}F)"
    }
}

data class RectArea(val left: Float, val right: Float, val top: Float, val bottom: Float)


fun getSortedItems(items: Map<RectArea, String>): List<Pair<RectArea, String>> =
    items.map { Pair(it.key, it.value) }.sortedBy { it.first.top }


fun calcRows(sortedItems: List<Pair<RectArea, String>>) : List<Range> {
    var top: Float = Float.MAX_VALUE
    var bottom: Float = Float.NEGATIVE_INFINITY
    val rows = mutableListOf<Range>()

    sortedItems.forEach {
        if (it.first.top <= bottom && it.first.bottom > bottom) {
            // extend row
            bottom = it.first.bottom
        } else if (it.first.top >= bottom) {
            if (top != Float.MAX_VALUE) {
                rows.add(Range(top, bottom))
            }
            top = it.first.top
            bottom = it.first.bottom
        }
    }
    if (top != Float.MAX_VALUE) {
        rows.add(Range(top, bottom))
    }

    return rows
}


fun rowContent(rowRanges: List<Range>,
                        sortedItems: List<Pair<RectArea, String>>,
                        rowFilter: (Set<String>) -> Boolean) : List<Map<Range, String>> {
    val logger = LoggerFactory.get(ImportDefinitionViewModel::class.simpleName!!)
    val rowContents = mutableListOf<Map<Range, String>>()
    var itemIndex = 0

    for (rowRange in rowRanges) {
        val rowItems = mutableMapOf<Range, String>()
        while (itemIndex <= sortedItems.lastIndex) {
            val item = sortedItems[itemIndex]
            val area = item.first
            if (area.top > rowRange.to) {
                break
            }
            val columnRange = Range(area.left, area.right)
            val current = rowItems[columnRange]
            rowItems[columnRange] = (current ?: "") + item.second
            itemIndex++
        }
        val mergedRowItems = mergeOverlapping(rowItems)
        if (rowFilter(mergedRowItems.map { it.value }.toSet())) {
            for (e in mergedRowItems) {
                logger.debug {"Pair(Range(${e.key.from}F, ${e.key.to}F), \"${e.value}\"),"}
            }
            rowContents.add(mergedRowItems)
        }
    }
    return rowContents
}

private fun mergeOverlapping(rowItems: Map<Range, String>) : Map<Range, String> {
    val sourceItemList = rowItems.entries.toList()
    val result = mutableMapOf<Range, String>()
    val merged = mutableSetOf<Int>()
    for ((index, entry) in sourceItemList.withIndex()) {
        if (merged.contains(index)) {
            continue
        }
        var from = entry.key.from
        var to = entry.key.to
        var value = entry.value

        for (index2 in (index + 1)..sourceItemList.lastIndex) {
            val e2 = sourceItemList[index2]
            if (e2.key.from < to && e2.key.to > from) {
                from = from.coerceAtLeast(e2.key.from)
                to = to.coerceAtMost(e2.key.to)
                value += e2.value
                merged.add(index2)
            }
        }
        result[Range(from, to)] = value
    }
    return result
}
