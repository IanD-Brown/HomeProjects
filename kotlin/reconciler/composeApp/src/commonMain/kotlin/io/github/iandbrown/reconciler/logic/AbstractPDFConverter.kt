package io.github.iandbrown.reconciler.logic

import dev.shivathapaa.logger.api.LoggerFactory
import io.github.iandbrown.reconciler.ui.ImportDefinitionViewModel
import kotlin.collections.iterator
import kotlin.collections.set

abstract class AbstractPDFConverter {
    fun rowContent(rowFilter: (Set<String>) -> Boolean): List<Map<Range, String>> {
        val sortedItems = getSortedItems(getItems())
        val rowRanges = calcRows(sortedItems)
        return rowContent(rowRanges, sortedItems, rowFilter)
    }

    fun getDateRange(): Pair<Int, Int> {
        val dateRangePattern = "(1st|2nd|3rd|\\d{1,2}th|31st)( [a-zA-Z]{3} )(\\d{4})( to )(1st|2nd|3rd|\\d{1,2}th|31st)( [a-zA-Z]{3} )(\\d{4})".toRegex()
        val dateResult = getItems().values.firstNotNullOf { dateRangePattern.matchEntire(it) }
        return Pair(dateResult.groupValues[3].toInt(), dateResult.groupValues[7].toInt())
    }

    protected abstract fun getItems() : Map<RectArea, String>
}

class TextAreaHolder {
    var dropThreshold = 0F
    var pageOffset = 0F
    var pageLowerLeftX: Float? = null
    var pageLowerLeftY: Float? = null
    var pageHeight: Float? = null
    val positions = mutableListOf<RectArea>()
    var currentText: String? = null
    val items = mutableMapOf<RectArea, String>()

    fun clear(dropThreshold: Float) {
        this.dropThreshold = dropThreshold
        items.clear()
        currentText = null
        positions.clear()
    }

    fun startPage(lowerLeftX: Float?, lowerLeftY: Float?, height: Float?) {
        if (pageHeight != null) {
            pageOffset += pageHeight!!
        }
        pageLowerLeftX = lowerLeftX
        pageLowerLeftY = lowerLeftY
        pageHeight = height
        saveCurrent()
    }

    fun saveCurrent() {
        if (currentText != null) {
            items[mergePositions()] = currentText!!
            currentText = null
        }
        positions.clear()
    }

    fun stringAt(text: String?, locations: Sequence<RectArea>) {
        if (currentText != null && text != null) {
            currentText += text
        } else {
            currentText = text
        }
        positions.addAll(locations)
    }

    private fun mergePositions() : RectArea {
        var left = Float.MAX_VALUE
        var top = Float.MAX_VALUE
        var right = Float.NEGATIVE_INFINITY
        var bottom = Float.NEGATIVE_INFINITY
        for (it in positions) {
            val x = it.left + pageLowerLeftX!!
            val y = pageOffset + it.top + pageLowerLeftY!!

            left = left.coerceAtMost(x)
            right = right.coerceAtLeast(x + (it.right - it.left))
            top = top.coerceAtMost(y)
            bottom = bottom.coerceAtLeast(y + (it.bottom - it.top))
        }

        return RectArea(left, right, top, bottom + dropThreshold)
    }
}

data class Range(val from: Float, val to: Float)

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
            try {
                for (e in mergedRowItems) {
                    logger.debug { "Pair(Range(${e.key.from}F, ${e.key.to}F), \"${e.value}\")," }
                }
            } catch (_: Exception) {
                // ignore, just a logging failure.
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
