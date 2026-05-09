package io.github.iandbrown.reconciler.logic

import dev.shivathapaa.logger.api.LoggerFactory
import io.github.iandbrown.reconciler.ui.ImportDefinitionViewModel
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.text.TextPosition
import java.io.IOException
import kotlin.collections.iterator

class PDFConverter : PDFConverterInterface {
    private val items: Map<RectArea, String>

    constructor(source: ByteArray) {
        val document = Loader.loadPDF(source)
        val textStripper = MyTextStripper()
        val logger = LoggerFactory.get(ImportDefinitionViewModel::class.simpleName!!)

        textStripper.startPage = 0
        textStripper.endPage = document.numberOfPages - 1
        textStripper.getText(document)
        for (e in textStripper.items) {
            logger.debug {"Pair(RectArea(${e.key.left}F, ${e.key.right}F, ${e.key.top}F, ${e.key.bottom}F), \"${e.value}\"),"}
        }

        items = textStripper.items

        document.close()
    }

    override fun rowContent(rowFilter: (Set<String>) -> Boolean) : List<Map<Range, String>> {
        val sortedItems = getSortedItems(items)
        val rowRanges = calcRows(sortedItems)
        return rowContent(rowRanges, sortedItems, rowFilter)
    }

    override fun getDateRange() : Pair<Int, Int> {
        val dateRangePattern = "(1st|2nd|3rd|\\d{1,2}th)( [a-zA-Z]{3} )(\\d{4})( to )(1st|2nd|3rd|\\d{1,2}th)( [a-zA-Z]{3} )(\\d{4})".toRegex()
        val dateResult = items.values.firstNotNullOf { dateRangePattern.matchEntire(it) }
        return Pair(dateResult.groupValues[3].toInt(), dateResult.groupValues[7].toInt())
    }
}

private class MyTextStripper : PDFTextStripper() {
    var cropBox: PDRectangle? = null
    var pageOffset = 0F
    val items = mutableMapOf<RectArea, String>()
    var currentText: String? = null
    val positions = mutableListOf<TextPosition?>()

    @Throws(IOException::class)
    override fun startPage(page: PDPage?) {
        if (cropBox != null) {
            pageOffset += cropBox?.height!!
        }
        cropBox = page?.cropBox
        saveCurrent()
        super.startPage(page)
    }

    private fun saveCurrent() {
        if (currentText != null) {
            items[toRectArea(positions)] = currentText!!
            currentText = null
        }
        positions.clear()
    }

    @Throws(IOException::class)
    override fun writeLineSeparator() {
        saveCurrent()
        super.writeLineSeparator()
    }

    @Throws(IOException::class)
    override fun getText(doc: PDDocument?): String? {
        sortByPosition = true
        items.clear()
        currentText = null
        positions.clear()
        return super.getText(doc)
    }

    @Throws(IOException::class)
    override fun writeWordSeparator() {
        saveCurrent()

        super.writeWordSeparator()
    }

    private fun toRectArea(textPositions: List<TextPosition?>) : RectArea {
        var left = Float.MAX_VALUE
        var top = Float.MAX_VALUE
        var right = Float.NEGATIVE_INFINITY
        var bottom = Float.NEGATIVE_INFINITY
        for (it in textPositions) {
            val x = it?.x!! + cropBox?.lowerLeftX!!
            val y = pageOffset + it.y + cropBox?.lowerLeftY!!

            left = left.coerceAtMost(x)
            right = right.coerceAtLeast(x + it.width)
            top = top.coerceAtMost(y)
            bottom = bottom.coerceAtLeast(y + it.height)
        }

        return RectArea(left, right, top, bottom + dropThreshold)
    }

    @Throws(IOException::class)
    override fun writeString(text: String?, textPositions: MutableList<TextPosition?>?) {
        if (currentText != null && text != null) {
            currentText += text
        } else {
            currentText = text
        }
        positions.addAll(textPositions!!)
        super.writeString(text, textPositions)
    }
}
